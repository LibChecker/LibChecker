package com.absinthe.libchecker.features.album.backup.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.databinding.ActivityBackupBinding
import com.absinthe.libchecker.features.home.ui.MainActivity
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.StorageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setBottomPaddingSpace
import com.absinthe.libchecker.utils.showToast
import com.google.android.material.card.MaterialCardView
import com.jakewharton.processphoenix.ProcessPhoenix
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import okio.source
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import timber.log.Timber

class BackupActivity : BaseActivity<ActivityBackupBinding>() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.album_item_backup_restore_title)

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, BackupFragment())
        .commit()
    }
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { intent?.data != null },
      handler = {
        val intent = Intent(this, MainActivity::class.java)
          // flags to bring MainActivity to the front and clear back stack
          .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
      }
    )
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  class BackupFragment : PreferenceFragmentCompat() {

    private val viewModel: SnapshotViewModel by viewModels()

    private lateinit var backupResultLauncher: ActivityResultLauncher<String>
    private lateinit var restoreResultLauncher: ActivityResultLauncher<String>
    private lateinit var roomBackup: RoomBackup
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
      return super.onCreateView(inflater, container, savedInstanceState).apply {
        addPaddingTop(96.dp)
      }
    }

    override fun onStart() {
      super.onStart()
      loadingDialog?.dismiss()
      loadingDialog = null
    }

    override fun onAttach(context: Context) {
      super.onAttach(context)
      backupResultLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
          it?.let {
            activity?.let { activity ->
              runCatching {
                val dialog = UiUtils.createLoadingDialog(activity)
                dialog.show()
                activity.contentResolver.openOutputStream(it)?.let { os ->
                  Timber.d("backupResultLauncher: openOutputStream")
                  viewModel.backup(os) {
                    dialog.dismiss()
                  }
                }
              }.onFailure { t ->
                Timber.e(t)
              }
            }
          }
        }
      restoreResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
          result?.let {
            restoreDatabase(it)
          }
        }
      roomBackup = RoomBackup(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.album_backup, null)
      preferenceScreen.applyM3eLayoutResources()

      findPreference<Preference>(Constants.PREF_LOCAL_BACKUP)?.apply {
        setOnPreferenceClickListener {
          val simpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
          val date = Date()
          val formatted = simpleDateFormat.format(date)

          if (StorageUtils.isExternalStorageWritable) {
            if (FileUtils.getFileSize(Repositories.getLCDatabaseFile()) > 100 * 1024 * 1024) {
              loadingDialog = UiUtils.createLoadingDialog(requireActivity())
              loadingDialog?.show()
              roomBackup
                .database(LCDatabase.getDatabase())
                .enableLogDebug(true)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .customBackupFileName("LibChecker-Snapshot-Backups-$formatted.sqlite3")
                .maxFileCount(5)
                .apply {
                  onCompleteListener { success, message, exitCode ->
                    Timber.d("success: $success, message: $message, exitCode: $exitCode")
                    lifecycleScope.launch(Dispatchers.Main) {
                      loadingDialog?.dismiss()
                      loadingDialog = null
                    }
                  }
                }
                .backup()
            } else {
              runCatching {
                backupResultLauncher.launch("LibChecker-Snapshot-Backups-$formatted.lcss")
              }.onFailure {
                Timber.e(it)
                context.showToast("Document API not working")
              }
            }
          } else {
            context.showToast("External storage is not writable")
          }
          true
        }
      }
      findPreference<Preference>(Constants.PREF_LOCAL_RESTORE)?.apply {
        setOnPreferenceClickListener {
          runCatching {
            restoreResultLauncher.launch("*/*")
          }.onFailure {
            Timber.e(it)
            context.showToast("Document API not working")
          }
          true
        }
      }

      activity?.intent?.data?.let { uri ->
        if (uri.scheme == "content" && uri.path?.endsWith(".lcss") == true) {
          restoreDatabase(uri)
        }
      }
    }

    override fun onCreateRecyclerView(
      inflater: LayoutInflater,
      parent: ViewGroup,
      savedInstanceState: Bundle?
    ): RecyclerView {
      val recyclerView = super.onCreateRecyclerView(
        inflater,
        parent,
        savedInstanceState
      ) as BorderRecyclerView
      recyclerView.id = android.R.id.list
      recyclerView.fixEdgeEffect()
      recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
      recyclerView.isVerticalScrollBarEnabled = false
      recyclerView.applySystemBarsPadding(bottom = true)

      doOnMainThreadIdle {
        recyclerView.setBottomPaddingSpace()
      }

      val lp = recyclerView.layoutParams
      if (lp is FrameLayout.LayoutParams) {
        lp.rightMargin = recyclerView.context.resources.getDimension(R.dimen.normal_padding).toInt()
        lp.leftMargin = lp.rightMargin
      }

      recyclerView.borderViewDelegate.borderVisibilityChangedListener =
        BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
          (activity as? BackupActivity)?.let {
            it.binding.appbar.isLifted = !top
          }
        }
      recyclerView.addOnChildAttachStateChangeListener(
        object : RecyclerView.OnChildAttachStateChangeListener {
          override fun onChildViewAttachedToWindow(view: View) {
            styleBackupPreferenceItem(recyclerView, view)
          }

          override fun onChildViewDetachedFromWindow(view: View) = Unit
        }
      )
      return recyclerView
    }

    private fun Preference.applyM3eLayoutResources() {
      when (this) {
        is PreferenceCategory -> {
          layoutResource = R.layout.preference_category_m3e
          isIconSpaceReserved = false
        }

        is SwitchPreferenceCompat -> {
          layoutResource = R.layout.preference_m3e
          widgetLayoutResource = R.layout.preference_widget_material_switch
          isIconSpaceReserved = true
        }

        else -> {
          layoutResource = R.layout.preference_m3e
          isIconSpaceReserved = true
        }
      }

      if (this is PreferenceGroup) {
        for (index in 0 until preferenceCount) {
          getPreference(index).applyM3eLayoutResources()
        }
      }
    }

    private fun styleBackupPreferenceItem(recyclerView: RecyclerView, itemView: View) {
      val adapter = recyclerView.adapter as? PreferenceGroupAdapter ?: return
      val position = recyclerView.getChildAdapterPosition(itemView)
      if (position == RecyclerView.NO_POSITION) return

      val preference = adapter.getItem(position)
      val card = itemView as? MaterialCardView ?: return
      if (!preference.isBackupRowPreference()) return

      val previous = if (position > 0) adapter.getItem(position - 1) else null
      val next = if (position < adapter.itemCount - 1) adapter.getItem(position + 1) else null
      val isFirstInGroup = !previous.isBackupRowPreference()
      val isLastInGroup = !next.isBackupRowPreference()
      val outerRadius = resources.getDimension(R.dimen.settings_preference_corner_radius)
      val innerRadius = resources.getDimension(R.dimen.settings_preference_inner_corner_radius)
      val topRadius = if (isFirstInGroup) outerRadius else innerRadius
      val bottomRadius = if (isLastInGroup) outerRadius else innerRadius

      card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
        .setTopLeftCornerSize(topRadius)
        .setTopRightCornerSize(topRadius)
        .setBottomLeftCornerSize(bottomRadius)
        .setBottomRightCornerSize(bottomRadius)
        .build()

      itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = if (isFirstInGroup) {
          0
        } else {
          resources.getDimensionPixelSize(R.dimen.settings_preference_card_spacing)
        }
        bottomMargin = 0
      }

      val hasSwitch = preference is TwoStatePreference
      itemView.findViewById<View>(android.R.id.widget_frame)?.isVisible = hasSwitch
      itemView.findViewById<View>(R.id.settings_preference_chevron)?.isVisible = false
    }

    private fun Preference?.isBackupRowPreference(): Boolean {
      return this != null && this !is PreferenceCategory
    }

    private fun restoreDatabase(uri: Uri) {
      activity?.let { activity ->
        runCatching {
          activity.contentResolver.openInputStream(uri)
            ?.let { inputStream ->
              val dialog = UiUtils.createLoadingDialog(activity)
              dialog.show()
              if (uri.toString().endsWith(".sqlite3")) {
                lifecycleScope.launch(Dispatchers.IO) {
                  val restoreFile = File(activity.externalCacheDir, "restore.sqlite3")
                  inputStream.source().buffer().use { source ->
                    restoreFile.outputStream().sink().buffer().use { sink ->
                      source.readAll(sink)
                    }
                  }
                  roomBackup
                    .database(LCDatabase.getDatabase())
                    .enableLogDebug(true)
                    .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                    .backupLocationCustomFile(restoreFile)
                    .apply {
                      onCompleteListener { success, message, exitCode ->
                        Timber.d("success: $success, message: $message, exitCode: $exitCode")
                        if (success) {
                          restoreFile.delete()
                          Once.clearDone(OnceTag.FIRST_LAUNCH)
                          ProcessPhoenix.triggerRebirth(LibCheckerApp.app)
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                          dialog.dismiss()
                        }
                      }
                    }
                    .restore()
                }
              } else {
                viewModel.restore(requireContext(), inputStream) { success ->
                  if (!success) {
                    context?.showToast("Backup file error")
                  }
                  dialog.dismiss()
                }
              }
            }
        }.onFailure { t ->
          Timber.e(t)
        }
      }
    }
  }
}
