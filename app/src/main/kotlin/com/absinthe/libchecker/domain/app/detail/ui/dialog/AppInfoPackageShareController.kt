package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareActionResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareExportResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareTarget
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class AppInfoPackageShareController(
  private val fragment: Fragment,
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val exportDocument: (Intent) -> Unit,
  private val dismiss: () -> Unit
) {
  private var pendingExportShareFile: AppPackageShareFile? = null
  private var pendingSharePackageName: String? = null
  private var loadingDialog: AlertDialog? = null

  init {
    collectPackageShareActionResults()
    collectPackageShareExportResults()
  }

  fun onExportResult(resultCode: Int, uri: Uri?) {
    if (resultCode != Activity.RESULT_OK || uri == null) {
      pendingExportShareFile = null
      return
    }
    val shareFile = pendingExportShareFile ?: run {
      pendingExportShareFile = null
      return
    }
    if (fragment.context == null) {
      pendingExportShareFile = null
      return
    }

    viewModel.exportAppPackageShareFile(shareFile, uri)
  }

  fun sharePackage(view: View, packageName: String?) {
    val pkg = packageName ?: run {
      Timber.e("package name is null")
      return
    }
    val activity = view.context as Activity
    pendingSharePackageName = pkg
    showLoading(activity)
    viewModel.prepareAppPackageShareAction(activity.cacheDir, pkg, AppPackageShareTarget.SHARE)
  }

  fun exportPackage(view: View, packageName: String?): Boolean {
    val pkg = packageName ?: run {
      Timber.e("package name is null")
      return true
    }

    val activity = view.context as Activity
    pendingSharePackageName = pkg
    showLoading(activity)
    viewModel.prepareAppPackageShareAction(activity.cacheDir, pkg, AppPackageShareTarget.EXPORT)

    return true
  }

  fun clear() {
    pendingSharePackageName = null
    pendingExportShareFile = null
    dismissLoading()
  }

  private fun collectPackageShareActionResults() {
    coroutineScope.launch {
      viewModel.appPackageShareActionResults.collect(::handlePackageShareActionResult)
    }
  }

  private fun handlePackageShareActionResult(loadResult: AppPackageShareActionResult) {
    if (loadResult.packageName != pendingSharePackageName) {
      return
    }
    pendingSharePackageName = null
    dismissLoading()
    val context = fragment.context ?: return
    loadResult.result.onFailure { throwable ->
      Timber.e(throwable)
      Toasty.showShort(context, throwable.toString())
    }
    val shareAction = loadResult.result.getOrNull() ?: return

    when (loadResult.target) {
      AppPackageShareTarget.SHARE -> {
        if (!GlobalValues.longTapShareButtonTip) {
          Toasty.showLong(context, R.string.toast_long_tap_share_button_tip)
        }
        runCatching {
          fragment.startActivity(Intent.createChooser(shareAction.shareIntent, null))
          Telemetry.recordEvent(
            Constants.Event.APP_INFO_BOTTOM_SHEET,
            mapOf(
              Telemetry.Param.CONTENT to "Unknown",
              "Action" to "Share"
            )
          )
          dismiss()
        }.onFailure { throwable ->
          Timber.e(throwable)
          Toasty.showShort(context, throwable.toString())
        }
      }

      AppPackageShareTarget.EXPORT -> {
        pendingExportShareFile = shareAction.shareFile
        runCatching {
          exportDocument(shareAction.exportIntent)
        }.onFailure { throwable ->
          pendingExportShareFile = null
          Timber.e(throwable)
          Toasty.showShort(context, throwable.toString())
        }.onSuccess {
          GlobalValues.longTapShareButtonTip = true
        }
      }
    }
  }

  private fun collectPackageShareExportResults() {
    coroutineScope.launch {
      viewModel.appPackageShareExportResults.collect(::handlePackageShareExportResult)
    }
  }

  private fun handlePackageShareExportResult(loadResult: AppPackageShareExportResult) {
    if (pendingExportShareFile != loadResult.shareFile) {
      return
    }
    val context = fragment.context
    loadResult.result.onSuccess {
      pendingExportShareFile = null
      Telemetry.recordEvent(
        Constants.Event.APP_INFO_BOTTOM_SHEET,
        mapOf(
          Telemetry.Param.CONTENT to "Unknown",
          "Action" to "Export APK"
        )
      )
      Timber.i("Exported APK for %s to %s", loadResult.shareFile.file.name, loadResult.destinationUri)
    }.onFailure { throwable ->
      pendingExportShareFile = null
      Timber.e(throwable, "Failed to export APK for %s", loadResult.shareFile.file.name)
      if (fragment.isAdded && context != null) {
        Toasty.showShort(context, R.string.toast_cant_open_app)
      }
    }
  }

  private fun showLoading(activity: Activity) {
    dismissLoading()
    loadingDialog = UiUtils.createLoadingDialog(activity).also {
      it.show()
    }
  }

  private fun dismissLoading() {
    loadingDialog?.dismiss()
    loadingDialog = null
  }
}
