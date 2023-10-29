package com.absinthe.libchecker.ui.detail

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageParser
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.showToast
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

class ApkDetailActivity : BaseAppDetailActivity(), IDetailContainer {

  private var tempFile: File? = null

  override val apkAnalyticsMode: Boolean = true
  override fun requirePackageName() = tempFile?.path
  override fun getToolbar() = binding.toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel.isApk = true
    resolveIntent(intent)
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
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
      runCatching {
        tempFile = File(externalCacheDir, Constants.TEMP_PACKAGE).also { tf ->
          contentResolver.openInputStream(uri)?.use { inputStream ->
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
                @Suppress("DEPRECATION", "InlinedApi")
                val flag = (
                  PackageManager.GET_SERVICES
                    or PackageManager.GET_ACTIVITIES
                    or PackageManager.GET_RECEIVERS
                    or PackageManager.GET_PROVIDERS
                    or PackageManager.GET_PERMISSIONS
                    or PackageManager.GET_META_DATA
                    or PackageManager.GET_SIGNATURES
                    or PackageManager.GET_SIGNING_CERTIFICATES
                    or PackageManager.MATCH_DISABLED_COMPONENTS
                    or PackageManager.MATCH_UNINSTALLED_PACKAGES
                  )
                try {
                  PackageManagerCompat.getPackageArchiveInfo(tf.path, flag)?.also {
                    it.applicationInfo.sourceDir = tf.path
                    it.applicationInfo.publicSourceDir = tf.path
                  }?.let { pi ->
                    onPackageInfoAvailable(pi, null)
                    dialog.dismiss()
                  } ?: run {
                    finish()
                  }
                } catch (e: Exception) {
                  Timber.e(e)
                  if (e is PackageParser.PackageParserException && e.message?.contains("Requires newer sdk version", true) == true) {
                    // bypass PackageParser check
                    // see also: https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/content/pm/PackageParser.java;l=2695
                    @Suppress("SoonBlockedPrivateApi")
                    PackageParser::class.java.getDeclaredField("SDK_VERSION").apply {
                      isAccessible = true
                      set(null, Integer.MAX_VALUE)
                    }

                    PackageManagerCompat.getPackageArchiveInfo(tf.path, flag)?.also {
                      it.applicationInfo.sourceDir = tf.path
                      it.applicationInfo.publicSourceDir = tf.path
                    }?.let { pi ->
                      onPackageInfoAvailable(pi, null)
                      dialog.dismiss()
                    } ?: run {
                      finish()
                    }

                    @Suppress("SoonBlockedPrivateApi")
                    PackageParser::class.java.getDeclaredField("SDK_VERSION").apply {
                      set(null, Build.VERSION.SDK_INT)
                      isAccessible = false
                    }
                  } else {
                    throw e
                  }
                }
              }
            } else {
              showToast(R.string.toast_not_enough_storage_space)
              finish()
            }
          }
        }
      }.onFailure {
        showToast(R.string.toast_use_another_file_manager)
        finish()
      }
    }
  }
}
