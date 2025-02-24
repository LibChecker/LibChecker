package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageParser.PackageParserException
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.apk.APKSParser
import com.absinthe.libchecker.utils.apk.XAPKParser
import com.absinthe.libchecker.utils.showToast
import java.io.File
import kotlinx.coroutines.Dispatchers
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
            finish()
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
      tempFile = File(externalCacheDir, Constants.TEMP_PACKAGE).also { tf ->
        if (tf.exists()) tf.delete()
        val inputStream = runCatching { contentResolver.openInputStream(uri) }.getOrNull() ?: run {
          dialog.dismiss()
          showToast(R.string.toast_use_another_file_manager)
          finish()
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

            withContext(Dispatchers.Main) {
              val flag = (
                PackageManager.GET_SERVICES
                  or PackageManager.GET_ACTIVITIES
                  or PackageManager.GET_RECEIVERS
                  or PackageManager.GET_PROVIDERS
                  or PackageManager.GET_PERMISSIONS
                  or PackageManager.GET_META_DATA
                  or PackageManager.MATCH_DISABLED_COMPONENTS
                  or PackageManager.MATCH_UNINSTALLED_PACKAGES
                )
              PackageManagerCompat.getPackageArchiveInfo(tf.path, flag)?.also {
                it.applicationInfo?.let { ai ->
                  ai.sourceDir = tf.path
                  ai.publicSourceDir = tf.path
                }
              }?.let { pi ->
                onPackageInfoAvailable(pi, null)
                dialog.dismiss()
              } ?: run {
                Timber.w("Failed to get PackageArchiveInfo")

                val result = runCatching {
                  val pi = initAPKSPackage(tf, flag)
                  onPackageInfoAvailable(pi, null)
                  dialog.dismiss()
                }
                if (result.isSuccess) return@run

                Timber.w("Not APKS file")
                val xapkResult = runCatching {
                  val pi = initXAPKPackage(tf, flag)
                  onPackageInfoAvailable(pi, null)
                  dialog.dismiss()
                }
                if (xapkResult.isSuccess) return@run

                Timber.w(xapkResult.exceptionOrNull(), "Failed to parse package")
                dialog.dismiss()
                showToast(R.string.toast_use_another_file_manager)
                finish()
              }
            }
          } else {
            dialog.dismiss()
            showToast(R.string.toast_not_enough_storage_space)
            finish()
          }
        }
      }
    }
  }

  private fun initXAPKPackage(file: File, flags: Int): PackageInfo {
    val parser = XAPKParser(file, flags)
    return parser.getPackageInfo() ?: throw PackageParserException()
  }

  private fun initAPKSPackage(file: File, flags: Int): PackageInfo {
    val parser = APKSParser(file, flags)
    return parser.getPackageInfo() ?: throw PackageParserException()
  }
}
