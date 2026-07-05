package com.absinthe.libchecker.domain.app.detail.ui

import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.ApkAnalysisPackageResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.ApkPreviewResult
import com.absinthe.libchecker.domain.app.detail.ui.IDetailContainer
import com.absinthe.libchecker.domain.app.packageinfo.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import java.io.File
import kotlinx.coroutines.launch
import timber.log.Timber

class ApkDetailActivity :
  BaseAppDetailActivity(),
  IDetailContainer {

  private var tempFile: File? = null
  private var loadingDialog: AlertDialog? = null

  override val apkAnalyticsMode: Boolean = true
  override fun requirePackageName() = tempFile?.path
  override fun getToolbar() = binding.toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    collectApkAnalysisPackageResults()
    collectApkPreviewResults()
    resolveIntent(intent)
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    super.onNewIntent(intent, caller)
    Timber.d("onNewIntent: $intent")
    resolveIntent(intent)
  }

  override fun onDestroy() {
    dismissLoadingDialog()
    tempFile?.delete()
    super.onDestroy()
  }

  private fun resolveIntent(intent: Intent?) {
    tempFile?.delete()
    tempFile = null
    intent?.let { i ->
      when {
        i.action == Intent.ACTION_SEND -> {
          IntentCompat.getParcelableExtra<Uri>(intent, Intent.EXTRA_STREAM)?.let { stream ->
            initPackage(stream)
          } ?: run {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
              initAPKPreview(url)
            } ?: run {
              finish()
            }
          }
        }

        i.data?.scheme == "content" -> {
          initPackage(i.data!!)
        }

        else -> {
          finish()
        }
      }
    } ?: run {
      finish()
    }
  }

  private fun initPackage(uri: Uri) {
    showLoadingDialog()
    viewModel.loadApkAnalysisPackage(requireAvailableCacheDir(), uri)
  }

  private fun initAPKPreview(url: String) {
    Timber.d("initAPKPreview: $url")
    showLoadingDialog()
    viewModel.loadApkPreview(url)
  }

  private fun collectApkAnalysisPackageResults() {
    lifecycleScope.launch {
      viewModel.apkAnalysisPackageResults.collect(::handleApkAnalysisPackageResult)
    }
  }

  private fun handleApkAnalysisPackageResult(loadResult: ApkAnalysisPackageResult) {
    when (val result = loadResult.result) {
      is PrepareApkAnalysisPackageUseCase.Result.Available -> {
        tempFile = result.file
        isPackageReady = true
        dismissLoadingDialog()
        onPackageInfoAvailable(result.packageInfo, null)
      }

      is PrepareApkAnalysisPackageUseCase.Result.InvalidPackage -> {
        tempFile = result.file
        dismissLoadingDialog()
        showToast(R.string.toast_use_another_file_manager)
        finish()
      }

      PrepareApkAnalysisPackageUseCase.Result.Unreadable -> {
        dismissLoadingDialog()
        showToast(R.string.toast_use_another_file_manager)
        finish()
      }

      PrepareApkAnalysisPackageUseCase.Result.NotEnoughStorage -> {
        dismissLoadingDialog()
        showToast(R.string.toast_not_enough_storage_space)
        finish()
      }
    }
  }

  private fun collectApkPreviewResults() {
    lifecycleScope.launch {
      viewModel.apkPreviewResults.collect(::handleApkPreviewResult)
    }
  }

  private fun handleApkPreviewResult(loadResult: ApkPreviewResult) {
    loadResult.result.onFailure {
      Timber.w(it, "Failed to preview APK from URL: ${loadResult.url}")
      dismissLoadingDialog()
      Toasty.showLong(this@ApkDetailActivity, it.toString())
      finish()
    }.getOrNull() ?: return

    onPackageInfoAvailable(PackageInfo(), null)
    dismissLoadingDialog()
  }

  private fun showLoadingDialog() {
    dismissLoadingDialog()
    loadingDialog = UiUtils.createLoadingDialog(this).also {
      it.show()
    }
  }

  private fun dismissLoadingDialog() {
    loadingDialog?.dismiss()
    loadingDialog = null
  }
}
