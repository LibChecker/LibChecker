package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import android.net.Uri
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.packageinfo.GetApkPreviewInfoUseCase
import com.absinthe.libchecker.domain.app.packageinfo.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.packageinfo.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import java.io.File

class DetailPackageLoader(
  private val getAppDetailPackage: GetAppDetailPackageUseCase,
  private val getApkPreviewInfoUseCase: GetApkPreviewInfoUseCase,
  private val prepareApkAnalysisPackageUseCase: PrepareApkAnalysisPackageUseCase,
  private val getInstalledAppComparisonPackageUseCase: GetInstalledAppComparisonPackageUseCase,
  private val buildPackageComparisonSnapshotItemUseCase: BuildPackageComparisonSnapshotItemUseCase
) {
  val packageState = DetailPackageState()

  val isApk: Boolean
    get() = packageState.isApk

  val isApkPreview: Boolean
    get() = packageState.isApkPreview

  val packageInfo: PackageInfo
    get() = packageState.packageInfo

  val apkPreviewInfo: ApkPreviewInfo?
    get() = packageState.apkPreviewInfo

  val packageInfoStateFlow = packageState.packageInfoStateFlow

  fun packageName(): String = packageState.packageName()

  fun initPackageInfo(packageInfo: PackageInfo) {
    packageState.setPackageInfo(packageInfo)
  }

  fun startApkMode() {
    packageState.startApkMode()
  }

  fun startApkPreviewMode() {
    packageState.startApkPreviewMode()
  }

  fun setApkPreviewInfo(apkPreviewInfo: ApkPreviewInfo) {
    packageState.apkPreviewInfo = apkPreviewInfo
  }

  fun clearApkPreviewInfo() {
    packageState.clearApkPreviewInfo()
  }

  fun isPackageInfoAvailable(): Boolean {
    return packageState.hasPackageInfo()
  }

  suspend fun loadAppDetailPackage(packageName: String): GetAppDetailPackageUseCase.Result {
    return getAppDetailPackage(packageName)
  }

  suspend fun getApkPreviewInfo(url: String): Result<ApkPreviewInfo> {
    return getApkPreviewInfoUseCase(url)
  }

  suspend fun prepareApkAnalysisPackage(
    cacheDir: File,
    uri: Uri
  ): PrepareApkAnalysisPackageUseCase.Result {
    return prepareApkAnalysisPackageUseCase(cacheDir, uri)
  }

  suspend fun isInstalledAppComparisonAvailable(packageName: String): Boolean {
    return getInstalledAppComparisonPackageUseCase.isAvailable(packageName)
  }

  suspend fun loadInstalledAppComparisonPackage(packageName: String): PackageInfo? {
    return getInstalledAppComparisonPackageUseCase(packageName)
  }

  suspend fun buildPackageComparisonSnapshotItem(
    basePackage: PackageInfo,
    analysisPackage: PackageInfo
  ): SnapshotDiffItem {
    return buildPackageComparisonSnapshotItemUseCase(basePackage, analysisPackage)
  }
}
