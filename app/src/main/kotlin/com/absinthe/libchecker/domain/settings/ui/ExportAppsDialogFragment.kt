package com.absinthe.libchecker.domain.settings.ui

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.settings.model.ExportAppsDialogAction
import com.absinthe.libchecker.domain.settings.model.ExportAppsDialogState
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ExportAppsDialogFragment : BaseBottomSheetViewDialogFragment<ExportAppsDialogView>() {

  private val viewModel: SettingsViewModel by viewModel(ownerProducer = { requireParentFragment() })
  private var exportJob: Job? = null
  private var dialogState: ExportAppsDialogState = ExportAppsDialogState.Ready

  private val createDocumentLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE_LCAPPS)) { uri ->
      if (uri == null) {
        render(ExportAppsDialogState.Ready)
        isCancelable = true
        return@registerForActivityResult
      }
      startExport(uri)
    }

  override fun initRootView(): ExportAppsDialogView = ExportAppsDialogView(requireContext())

  override fun init() {
    root.bind(dialogState, ::handleAction)
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onDestroyView() {
    exportJob?.cancel()
    super.onDestroyView()
  }

  private fun handleAction(action: ExportAppsDialogAction) {
    when (action) {
      ExportAppsDialogAction.PrimaryButtonClick -> {
        when (dialogState) {
          ExportAppsDialogState.Ready -> {
            render(ExportAppsDialogState.Preparing)
            isCancelable = false
            createDocumentLauncher.launch(viewModel.buildInstalledAppsExportFileName())
          }

          ExportAppsDialogState.Done -> dismissAllowingStateLoss()

          ExportAppsDialogState.Preparing,
          is ExportAppsDialogState.Exporting -> Unit
        }
      }
    }
  }

  private fun startExport(uri: Uri) {
    exportJob?.cancel()
    exportJob = viewLifecycleOwner.lifecycleScope.launch {
      render(ExportAppsDialogState.Exporting(progress = 0))
      try {
        val result = viewModel.exportInstalledApps(uri) { progress ->
          withContext(Dispatchers.Main) {
            render(ExportAppsDialogState.Exporting(progress))
          }
        }
        render(ExportAppsDialogState.Done)
        Toasty.showShort(requireContext(), getString(R.string.export_apps_success, result.appCount))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Throwable) {
        Timber.e(e)
        render(ExportAppsDialogState.Ready)
        Toasty.showShort(
          requireContext(),
          getString(R.string.export_apps_failed, e.localizedMessage ?: e.toString())
        )
      } finally {
        isCancelable = true
      }
    }
  }

  private fun render(state: ExportAppsDialogState) {
    dialogState = state
    root.bind(state, ::handleAction)
  }

  private companion object {
    const val MIME_TYPE_LCAPPS = "application/octet-stream"
  }
}
