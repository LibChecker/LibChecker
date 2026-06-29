package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
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

  fun onExportResult(resultCode: Int, uri: Uri?) {
    if (resultCode != Activity.RESULT_OK || uri == null) {
      pendingExportShareFile = null
      return
    }
    val shareFile = pendingExportShareFile ?: run {
      pendingExportShareFile = null
      return
    }
    val context = fragment.context ?: run {
      pendingExportShareFile = null
      return
    }

    coroutineScope.launch {
      runCatching {
        viewModel.exportAppPackageShareFile(shareFile, uri)
      }.onSuccess {
        pendingExportShareFile = null
        Telemetry.recordEvent(
          Constants.Event.APP_INFO_BOTTOM_SHEET,
          mapOf(
            Telemetry.Param.CONTENT to "Unknown",
            "Action" to "Export APK"
          )
        )
        Timber.i("Exported APK for %s to %s", shareFile.file.name, uri)
      }.onFailure { throwable ->
        pendingExportShareFile = null
        Timber.e(throwable, "Failed to export APK for %s", shareFile.file.name)
        if (fragment.isAdded) {
          Toasty.showShort(context, R.string.toast_cant_open_app)
        }
      }
    }
  }

  fun sharePackage(view: View, packageName: String?) {
    val pkg = packageName ?: run {
      Timber.e("package name is null")
      return
    }
    val activity = view.context as Activity
    val loading = UiUtils.createLoadingDialog(activity)
    loading.show()
    coroutineScope.launch {
      val shareActionResult = runCatching {
        viewModel.prepareAppPackageShareAction(activity.cacheDir, pkg)
      }
      loading.dismiss()

      shareActionResult.onSuccess { shareAction ->
        if (!GlobalValues.longTapShareButtonTip) {
          Toasty.showLong(view.context, R.string.toast_long_tap_share_button_tip)
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
          Toasty.showShort(activity, throwable.toString())
        }
      }.onFailure { throwable ->
        Timber.e(throwable)
        Toasty.showShort(activity, throwable.toString())
      }
    }
  }

  fun exportPackage(view: View, packageName: String?): Boolean {
    val pkg = packageName ?: run {
      Timber.e("package name is null")
      return true
    }

    coroutineScope.launch {
      val activity = view.context as Activity
      val loading = UiUtils.createLoadingDialog(activity)
      loading.show()
      val shareActionResult = runCatching {
        viewModel.prepareAppPackageShareAction(activity.cacheDir, pkg)
      }
      loading.dismiss()

      shareActionResult.onSuccess { shareAction ->
        pendingExportShareFile = shareAction.shareFile
        runCatching {
          exportDocument(shareAction.exportIntent)
        }.onFailure { throwable ->
          pendingExportShareFile = null
          Timber.e(throwable)
          Toasty.showShort(activity, throwable.toString())
        }.onSuccess {
          GlobalValues.longTapShareButtonTip = true
        }
      }.onFailure { throwable ->
        Timber.e(throwable)
        Toasty.showShort(activity, throwable.toString())
      }
    }

    return true
  }
}
