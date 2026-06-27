package com.absinthe.libchecker.domain.snapshot.backup.ui

import android.annotation.SuppressLint
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
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.databinding.ActivityBackupBinding
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.StorageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.extensions.setBottomPaddingSpace
import com.absinthe.libchecker.utils.showToast
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
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

    private val viewModel: SnapshotBackupViewModel by viewModel()

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
                viewModel.backup(it) {
                  dialog.dismiss()
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
            restoreBackup(it)
          }
        }
      roomBackup = RoomBackup(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.album_backup, null)
      preferenceScreen.applyM3eLayoutResources()

      findPreference<Preference>(Constants.PREF_LOCAL_BACKUP)?.apply {
        setOnPreferenceClickListener {
          val action = viewModel.onLocalBackupRequested(StorageUtils.isExternalStorageWritable)
          when (action) {
            is SnapshotBackupViewModel.LocalBackupAction.CreateArchive -> launchArchiveBackup(action.fileName)

            SnapshotBackupViewModel.LocalBackupAction.CreateDatabase -> createDatabaseBackup()

            SnapshotBackupViewModel.LocalBackupAction.StorageUnavailable ->
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
        if (viewModel.shouldRestoreFromLaunchUri(uri)) {
          restoreBackup(uri)
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

    @SuppressLint("RestrictedApi")
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
      itemView.findViewById<View>(android.R.id.title)?.importantForAccessibility =
        View.IMPORTANT_FOR_ACCESSIBILITY_NO
      itemView.findViewById<View>(android.R.id.summary)?.importantForAccessibility =
        View.IMPORTANT_FOR_ACCESSIBILITY_NO
      itemView.contentDescription = buildPreferenceDescription(preference)
    }

    private fun Preference?.isBackupRowPreference(): Boolean {
      return this != null && this !is PreferenceCategory
    }

    private fun buildPreferenceDescription(preference: Preference?): String {
      val parts = mutableListOf<CharSequence?>(
        preference?.title,
        preference?.summary
      )
      if (preference is TwoStatePreference) {
        parts += getString(
          if (preference.isChecked) {
            R.string.array_dark_mode_on
          } else {
            R.string.array_dark_mode_off
          }
        )
      }
      return parts
        .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
        .joinToString()
    }

    private fun restoreBackup(uri: Uri) {
      activity?.let { activity ->
        runCatching {
          val dialog = UiUtils.createLoadingDialog(activity)
          dialog.show()
          viewModel.restoreBackup(
            roomBackup = roomBackup,
            uri = uri,
            cacheDir = activity.requireAvailableCacheDir()
          ) { result ->
            dialog.dismiss()
            when (result) {
              SnapshotBackupViewModel.RestoreBackupResult.DatabaseBackup -> Unit

              is SnapshotBackupViewModel.RestoreBackupResult.ArchiveBackup -> {
                val summary = result.summary
                if (summary == null) {
                  context?.showToast("Backup file error")
                } else {
                  showRestoreResultDialog(summary)
                }
              }
            }
          }
        }.onFailure { t ->
          Timber.e(t)
        }
      }
    }

    private fun launchArchiveBackup(fileName: String) {
      runCatching {
        backupResultLauncher.launch(fileName)
      }.onFailure {
        Timber.e(it)
        context?.showToast("Document API not working")
      }
    }

    private fun createDatabaseBackup() {
      loadingDialog = UiUtils.createLoadingDialog(requireActivity())
      loadingDialog?.show()
      viewModel.createDatabaseBackup(roomBackup) { result ->
        Timber.d(
          "success: ${result.success}, message: ${result.message}, exitCode: ${result.exitCode}"
        )
        lifecycleScope.launch(Dispatchers.Main) {
          loadingDialog?.dismiss()
          loadingDialog = null
        }
      }.onFailure {
        loadingDialog?.dismiss()
        loadingDialog = null
      }
    }

    private fun showRestoreResultDialog(summary: SnapshotBackupViewModel.ArchiveRestoreSummary) {
      val fragmentContext = context ?: return
      val message = buildString {
        summary.items.forEach {
          append(
            fragmentContext.getString(
              R.string.album_restore_detail,
              it.formattedTimestamp,
              it.count.toString()
            )
          )
        }
      }
      BaseAlertDialogBuilder(fragmentContext)
        .setTitle(R.string.album_restore)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ -> }
        .setCancelable(true)
        .show()
    }
  }
}
