package com.absinthe.libchecker.domain.snapshot.backup.ui

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotArchiveBackupResult
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SnapshotBackupBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<SnapshotBackupBottomSheetView>() {

  private val viewModel: SnapshotBackupViewModel by viewModel()
  private lateinit var roomBackup: RoomBackup
  private var loadingDialog: AlertDialog? = null
  private val pendingRestoreUris = ArrayDeque<Uri>()
  private var activeRestoreUri: Uri? = null

  private val backupResultLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
      uri?.let(::backupArchive)
    }

  private val restoreResultLauncher =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      uri?.let { enqueueRestoreUri(it, requireLaunchUri = false) }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val savedRestoreUris = savedInstanceState?.getStringArrayList(STATE_RESTORE_URIS)
    if (savedRestoreUris == null) {
      arguments?.getString(ARG_RESTORE_URI)?.toUri()?.let {
        enqueueRestoreUri(it, requireLaunchUri = true)
      }
    } else {
      savedRestoreUris
        .asSequence()
        .map(String::toUri)
        .take(MAX_PENDING_RESTORE_URIS)
        .forEach(pendingRestoreUris::addLast)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    val restoreUris = ArrayList<String>(pendingRestoreUris.size)
    pendingRestoreUris.mapTo(restoreUris, Uri::toString)
    outState.putStringArrayList(STATE_RESTORE_URIS, restoreUris)
    super.onSaveInstanceState(outState)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    roomBackup = (context as? SnapshotRoomBackupOwner)?.snapshotRoomBackup
      ?: error("${context::class.java.simpleName} must implement SnapshotRoomBackupOwner")
  }

  override fun initRootView(): SnapshotBackupBottomSheetView {
    return SnapshotBackupBottomSheetView(requireContext())
  }

  override fun init() {
    maxPeekHeightPercentage = 0.82f
    isInitialLandscapeExpansionEnabled = false
    root.bind(::handleAction)
    val runningRestoreUri = viewModel.getActiveRestoreUri()
    if (runningRestoreUri == null) {
      consumePendingRestoreUri()
    } else {
      activeRestoreUri = runningRestoreUri
      root.post { restoreBackup(runningRestoreUri) }
    }
  }

  fun restoreFromLaunchUri(uri: Uri) {
    enqueueRestoreUri(uri, requireLaunchUri = true)
  }

  private fun enqueueRestoreUri(uri: Uri, requireLaunchUri: Boolean) {
    if (requireLaunchUri && !viewModel.shouldRestoreFromLaunchUri(uri)) {
      Timber.w("Ignoring unsupported snapshot restore URI")
      return
    }
    if (
      uri == activeRestoreUri ||
      pendingRestoreUris.contains(uri) ||
      pendingRestoreUris.size >= MAX_PENDING_RESTORE_URIS
    ) {
      Timber.w("Ignoring duplicate or excess snapshot restore URI")
      return
    }
    viewModel.invalidateCompletedRestoreResult()
    pendingRestoreUris.addLast(uri)
    if (view != null && activeRestoreUri == null) {
      consumePendingRestoreUri()
    }
  }

  private fun consumePendingRestoreUri() {
    val uri = takeNextPendingSnapshotRestoreRequest(
      pendingRestoreUris = pendingRestoreUris,
      activeRestoreUri = activeRestoreUri
    ) ?: return
    activeRestoreUri = uri
    root.post { restoreBackup(uri) }
  }

  override fun onDestroyView() {
    loadingDialog?.dismiss()
    loadingDialog = null
    super.onDestroyView()
  }

  private fun handleAction(action: SnapshotBackupBottomSheetAction) {
    when (action) {
      SnapshotBackupBottomSheetAction.Backup -> requestBackup()
      SnapshotBackupBottomSheetAction.Restore -> launchRestorePicker()
    }
  }

  private fun requestBackup() {
    lifecycleScope.launch {
      when (
        val action = viewModel.onLocalBackupRequested(
          Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        )
      ) {
        is SnapshotBackupViewModel.LocalBackupAction.CreateArchive ->
          launchArchiveBackup(action.fileName)

        SnapshotBackupViewModel.LocalBackupAction.CreateDatabase ->
          createDatabaseBackup()

        SnapshotBackupViewModel.LocalBackupAction.StorageUnavailable ->
          context?.showToast("External storage is not writable")

        SnapshotBackupViewModel.LocalBackupAction.NoSnapshot ->
          context?.showToast(R.string.snapshot_no_snapshot)
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

  private fun launchRestorePicker() {
    runCatching {
      restoreResultLauncher.launch("*/*")
    }.onFailure {
      Timber.e(it)
      context?.showToast("Document API not working")
    }
  }

  private fun backupArchive(uri: Uri) {
    val activity = activity ?: return
    showLoadingDialog()
    viewModel.backup(uri) { result ->
      dismissLoadingDialog()
      when (result) {
        is SnapshotArchiveBackupResult.Success -> {
          showBackupResultDialog(
            activity.getString(R.string.album_backup_detail, result.itemCount)
          )
        }

        SnapshotArchiveBackupResult.Empty ->
          context?.showToast(R.string.snapshot_no_snapshot)

        SnapshotArchiveBackupResult.Failed ->
          context?.showToast("Backup file error")
      }
    }
  }

  private fun createDatabaseBackup() {
    showLoadingDialog()
    viewModel.createDatabaseBackup(roomBackup) { result ->
      Timber.d(
        "success: ${result.success}, message: ${result.message}, exitCode: ${result.exitCode}"
      )
      lifecycleScope.launch(Dispatchers.Main) {
        dismissLoadingDialog()
        if (result.success) {
          showBackupResultDialog(getString(R.string.album_backup_database_detail))
        }
      }
    }.onFailure {
      dismissLoadingDialog()
    }
  }

  private fun restoreBackup(uri: Uri) {
    val activity = activity ?: return
    activeRestoreUri = uri
    showLoadingDialog()
    viewModel.restoreBackup(
      roomBackup = roomBackup,
      uri = uri,
      cacheDir = activity.requireAvailableCacheDir()
    ) { result ->
      activeRestoreUri = null
      dismissLoadingDialog()
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
      if (view != null) {
        consumePendingRestoreUri()
      }
    }
  }

  private fun showLoadingDialog() {
    loadingDialog?.dismiss()
    loadingDialog = activity?.let(UiUtils::createLoadingDialog)?.also { it.show() }
  }

  private fun dismissLoadingDialog() {
    loadingDialog?.dismiss()
    loadingDialog = null
  }

  private fun showRestoreResultDialog(
    summary: SnapshotBackupViewModel.ArchiveRestoreSummary
  ) {
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
    val onSurfaceVariant =
      context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
    val bodyMedium =
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium)
    val titleSmall =
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall)
    val labelLarge =
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelLarge)

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
          layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
          )
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

  companion object {
    private const val ARG_RESTORE_URI = "restore_uri"
    private const val STATE_RESTORE_URIS = "restore_uri_states"
    private const val MAX_PENDING_RESTORE_URIS = 1

    fun newInstance(restoreUri: Uri? = null): SnapshotBackupBottomSheetDialogFragment {
      return SnapshotBackupBottomSheetDialogFragment().apply {
        arguments = Bundle().apply {
          restoreUri?.let { putString(ARG_RESTORE_URI, it.toString()) }
        }
      }
    }
  }
}

internal fun <T> takeNextPendingSnapshotRestoreRequest(
  pendingRestoreUris: ArrayDeque<T>,
  activeRestoreUri: T?
): T? {
  if (activeRestoreUri != null) {
    return null
  }
  return pendingRestoreUris.removeFirstOrNull()
}

interface SnapshotRoomBackupOwner {
  val snapshotRoomBackup: RoomBackup
}
