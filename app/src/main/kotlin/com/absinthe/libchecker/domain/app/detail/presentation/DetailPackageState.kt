package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import kotlinx.coroutines.flow.MutableStateFlow

class DetailPackageState {
  var isApk = false
    private set
  var isApkPreview = false
    private set

  lateinit var packageInfo: PackageInfo
    private set

  var apkPreviewInfo: ApkPreviewInfo? = null
  val packageInfoStateFlow = MutableStateFlow<PackageInfo?>(null)

  fun setPackageInfo(packageInfo: PackageInfo) {
    this.packageInfo = packageInfo
    packageInfoStateFlow.value = packageInfo
  }

  fun startApkMode() {
    isApk = true
    isApkPreview = false
  }

  fun startApkPreviewMode() {
    isApk = false
    isApkPreview = true
  }

  fun clearApkPreviewInfo() {
    apkPreviewInfo = null
  }

  fun hasPackageInfo(): Boolean {
    return this::packageInfo.isInitialized
  }

  fun packageName(): String {
    return apkPreviewInfo?.packageName ?: packageInfo.packageName
  }
}
