package com.absinthe.libchecker.domain.snapshot.backup.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.databinding.ActivityBackupBinding
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotArchiveBackupResult
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.preference.applyM3eLayoutResources
import com.absinthe.libchecker.ui.preference.buildPreferenceItemRenderState
import com.absinthe.libchecker.ui.preference.view.PreferenceItemView
import com.absinthe.libchecker.utils.StorageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.extensions.setBottomPaddingSpace
import com.absinthe.libchecker.utils.showToast
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
                viewModel.backup(it) { result ->
                  dialog.dismiss()
                  when (result) {
                    is SnapshotArchiveBackupResult.Success -> showBackupResultDialog(
                      getString(R.string.album_backup_detail, result.itemCount)
                    )

                    SnapshotArchiveBackupResult.Empty -> context.showToast(R.string.snapshot_no_snapshot)

                    SnapshotArchiveBackupResult.Failed -> context.showToast("Backup file error")
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
          lifecycleScope.launch {
            val action = viewModel.onLocalBackupRequested(StorageUtils.isExternalStorageWritable)
            when (action) {
              is SnapshotBackupViewModel.LocalBackupAction.CreateArchive -> launchArchiveBackup(action.fileName)

              SnapshotBackupViewModel.LocalBackupAction.CreateDatabase -> createDatabaseBackup()

              SnapshotBackupViewModel.LocalBackupAction.StorageUnavailable ->
                context.showToast("External storage is not writable")

              SnapshotBackupViewModel.LocalBackupAction.NoSnapshot ->
                context.showToast(R.string.snapshot_no_snapshot)
            }
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
            bindBackupPreferenceItem(recyclerView, view)
          }

          override fun onChildViewDetachedFromWindow(view: View) = Unit
        }
      )
      return recyclerView
    }

    private fun bindBackupPreferenceItem(recyclerView: RecyclerView, itemView: View) {
      val preferenceItemView = itemView as? PreferenceItemView ?: return
      val adapter = recyclerView.adapter as? PreferenceGroupAdapter ?: return
      val position = recyclerView.getChildAdapterPosition(itemView)
      if (position == RecyclerView.NO_POSITION) return
      val state = adapter.buildPreferenceItemRenderState(position) ?: return
      preferenceItemView.bind(state)
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
              is SnapshotBackupViewModel.RestoreBackupResult.DatabaseBackup -> {
                if (result.success.not()) {
                  context?.showToast("Backup file error")
                }
              }

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
          if (result.success) {
            showBackupResultDialog(getString(R.string.album_backup_database_detail))
          }
        }
      }.onFailure {
        loadingDialog?.dismiss()
        loadingDialog = null
      }
    }

    private fun showRestoreResultDialog(summary: SnapshotBackupViewModel.ArchiveRestoreSummary) {
      val fragmentContext = context ?: return
      BaseAlertDialogBuilder(fragmentContext)
        .setTitle(R.string.album_restore)
        .setView(buildRestoreResultView(fragmentContext, summary))
        .setPositiveButton(android.R.string.ok) { _, _ -> }
        .setCancelable(true)
        .show()
    }

    private fun buildRestoreResultView(
      context: Context,
      summary: SnapshotBackupViewModel.ArchiveRestoreSummary
    ): View {
      val totalCount = summary.items.sumOf { it.count }
      val onSurfaceVariant = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
      val bodyMedium = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium)
      val titleSmall = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall)
      val labelLarge = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelLarge)

      return ScrollView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addView(
          LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 4.dp, 24.dp, 0)
            addView(
              AppCompatTextView(context).apply {
                setTextAppearance(bodyMedium)
                setTextColor(onSurfaceVariant)
                text = context.getString(
                  R.string.album_restore_result_summary,
                  totalCount,
                  summary.items.size
                )
              }
            )
            summary.items.forEachIndexed { index, item ->
              addView(
                buildRestoreResultRow(
                  context = context,
                  item = item,
                  titleTextAppearance = titleSmall,
                  countTextAppearance = labelLarge,
                  countTextColor = onSurfaceVariant
                ).apply {
                  updateLayoutParams<LinearLayout.LayoutParams> {
                    topMargin = if (index == 0) 16.dp else 12.dp
                  }
                }
              )
            }
          }
        )
      }
    }

    private fun buildRestoreResultRow(
      context: Context,
      item: SnapshotBackupViewModel.ArchiveRestoreSummaryItem,
      titleTextAppearance: Int,
      countTextAppearance: Int,
      countTextColor: Int
    ): View {
      return LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        )
        addView(
          AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            setTextAppearance(titleTextAppearance)
            text = item.formattedTimestamp
          }
        )
        addView(
          AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
              it.marginStart = 16.dp
            }
            gravity = Gravity.END
            setTextAppearance(countTextAppearance)
            setTextColor(countTextColor)
            setTypeface(typeface, Typeface.BOLD)
            text = context.getString(R.string.album_restore_result_item_count, item.count)
          }
        )
      }
    }

    private fun showBackupResultDialog(message: CharSequence) {
      val fragmentContext = context ?: return
      BaseAlertDialogBuilder(fragmentContext)
        .setTitle(R.string.album_backup)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ -> }
        .setCancelable(true)
        .show()
    }
  }
}
