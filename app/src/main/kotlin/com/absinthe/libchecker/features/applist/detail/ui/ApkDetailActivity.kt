package com.absinthe.libchecker.features.applist.detail.ui

import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
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
    viewModel.isApk = true
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
    val dialog = UiUtils.createLoadingDialog(this)
    dialog.show()

    lifecycleScope.launch(Dispatchers.IO) {
      tempFile = File(requireAvailableCacheDir(), Constants.TEMP_PACKAGE).also { tf ->
        if (tf.exists()) tf.delete()
        val inputStream = runCatching { contentResolver.openInputStream(uri) }.getOrNull() ?: run {
          withContext(Dispatchers.Main) {
            dialog.dismiss()
            showToast(R.string.toast_use_another_file_manager)
            finish()
          }
          return@launch
        }
        inputStream.use {
          val fileSize = inputStream.available()
          val freeSize = Environment.getExternalStorageDirectory().freeSpace
          Timber.d("fileSize=$fileSize, freeSize=$freeSize")

          if (freeSize > fileSize * 1.5) {
            tf.sink().buffer().use { sink ->
              inputStream.source().buffer().use {
                sink.writeAll(it)
              }
            }
            isPackageReady = true

            val packageInfo = viewModel.getArchivePackageInfo(tf)
            withContext(Dispatchers.Main) {
              dialog.dismiss()
              if (packageInfo != null) {
                onPackageInfoAvailable(packageInfo, null)
              } else {
                showToast(R.string.toast_use_another_file_manager)
                finish()
              }
            }
          } else {
            withContext(Dispatchers.Main) {
              dialog.dismiss()
              showToast(R.string.toast_not_enough_storage_space)
              finish()
            }
          }
        }
      }
    }
  }

  private fun initAPKPreview(url: String) {
    Timber.d("initAPKPreview: $url")
    viewModel.isApk = false
    viewModel.isApkPreview = true

    val dialog = UiUtils.createLoadingDialog(this)
    dialog.show()

    lifecycleScope.launch {
      val previewInfo = viewModel.getApkPreviewInfo(url).onFailure {
        Timber.w(it, "Failed to preview APK from URL: $url")
        dialog.dismiss()
        Toasty.showLong(this@ApkDetailActivity, it.toString())
        finish()
      }.getOrNull() ?: return@launch
      viewModel.apkPreviewInfo = previewInfo

      onPackageInfoAvailable(PackageInfo(), null)
      dialog.dismiss()
    }
  }
}
