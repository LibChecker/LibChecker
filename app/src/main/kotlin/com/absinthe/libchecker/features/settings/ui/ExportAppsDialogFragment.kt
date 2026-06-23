package com.absinthe.libchecker.features.settings.ui

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.ExportInstalledAppsToUriUseCase
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber

class ExportAppsDialogFragment : BaseBottomSheetViewDialogFragment<ExportAppsDialogView>() {

  private val exportInstalledAppsToUriUseCase: ExportInstalledAppsToUriUseCase by inject()
  private var exportJob: Job? = null
  private var isExporting = false

  private val createDocumentLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE_LCAPPS)) { uri ->
      if (uri == null) {
        root.showReady()
        isCancelable = true
        return@registerForActivityResult
      }
      startExport(uri)
    }

  override fun initRootView(): ExportAppsDialogView = ExportAppsDialogView(requireContext())

  override fun init() {
    root.exportButton.setOnClickListener {
      if (isExporting) {
        return@setOnClickListener
      }
      root.showPreparing()
      isCancelable = false
      createDocumentLauncher.launch(buildDefaultFileName())
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onDestroyView() {
    exportJob?.cancel()
    super.onDestroyView()
  }

  private fun startExport(uri: Uri) {
    exportJob?.cancel()
    exportJob = viewLifecycleOwner.lifecycleScope.launch {
      isExporting = true
      root.showExporting()
      try {
        val result = exportInstalledAppsToUriUseCase(uri) { progress ->
          withContext(Dispatchers.Main) {
            root.setProgress(progress)
          }
        }
        root.showDone()
        root.exportButton.setOnClickListener { dismissAllowingStateLoss() }
        Toasty.showShort(requireContext(), getString(R.string.export_apps_success, result.appCount))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Throwable) {
        Timber.e(e)
        root.showReady()
        root.exportButton.setOnClickListener {
          if (isExporting) {
            return@setOnClickListener
          }
          root.showPreparing()
          isCancelable = false
          createDocumentLauncher.launch(buildDefaultFileName())
        }
        Toasty.showShort(
          requireContext(),
          getString(R.string.export_apps_failed, e.localizedMessage ?: e.toString())
        )
      } finally {
        isExporting = false
        isCancelable = true
      }
    }
  }

  private fun buildDefaultFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "LibChecker-$timestamp.lcapps"
  }

  private companion object {
    const val MIME_TYPE_LCAPPS = "application/octet-stream"
  }
}
