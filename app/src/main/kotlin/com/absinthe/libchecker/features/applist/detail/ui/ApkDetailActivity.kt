package com.absinthe.libchecker.features.applist.detail.ui

import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.domain.app.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
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

  override val apkAnalyticsMode: Boolean = true
  override fun requirePackageName() = tempFile?.path
  override fun getToolbar() = binding.toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel.startApkMode()
    viewModel.clearApkPreviewInfo()
    resolveIntent(intent)
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    super.onNewIntent(intent, caller)
    Timber.d("onNewIntent: $intent")
    resolveIntent(intent)
  }

  override fun onDestroy() {
    tempFile?.delete()
    super.onDestroy()
  }

  private fun resolveIntent(intent: Intent?) {
    tempFile?.delete()
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
    viewModel.startApkMode()
    viewModel.clearApkPreviewInfo()

    val dialog = UiUtils.createLoadingDialog(this)
    dialog.show()

    lifecycleScope.launch {
      when (val result = viewModel.prepareApkAnalysisPackage(requireAvailableCacheDir(), uri)) {
        is PrepareApkAnalysisPackageUseCase.Result.Available -> {
          tempFile = result.file
          isPackageReady = true
          dialog.dismiss()
          onPackageInfoAvailable(result.packageInfo, null)
        }

        is PrepareApkAnalysisPackageUseCase.Result.InvalidPackage -> {
          tempFile = result.file
          dialog.dismiss()
          showToast(R.string.toast_use_another_file_manager)
          finish()
        }

        PrepareApkAnalysisPackageUseCase.Result.Unreadable -> {
          dialog.dismiss()
          showToast(R.string.toast_use_another_file_manager)
          finish()
        }

        PrepareApkAnalysisPackageUseCase.Result.NotEnoughStorage -> {
          dialog.dismiss()
          showToast(R.string.toast_not_enough_storage_space)
          finish()
        }
      }
    }
  }

  private fun initAPKPreview(url: String) {
    Timber.d("initAPKPreview: $url")
    viewModel.startApkPreviewMode()

    val dialog = UiUtils.createLoadingDialog(this)
    dialog.show()

    lifecycleScope.launch {
      val previewInfo = viewModel.getApkPreviewInfo(url).onFailure {
        Timber.w(it, "Failed to preview APK from URL: $url")
        dialog.dismiss()
        Toasty.showLong(this@ApkDetailActivity, it.toString())
        finish()
      }.getOrNull() ?: return@launch
      viewModel.setApkPreviewInfo(previewInfo)

      onPackageInfoAvailable(PackageInfo(), null)
      dialog.dismiss()
    }
  }
}
