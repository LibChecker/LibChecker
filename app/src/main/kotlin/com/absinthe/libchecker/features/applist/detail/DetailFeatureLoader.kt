package com.absinthe.libchecker.features.applist.detail

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.BuildAppDetailAbiLabelDataUseCase
import com.absinthe.libchecker.domain.app.BuildAppDetailHeaderExtraInfoUseCase
import com.absinthe.libchecker.domain.app.BuildAppDetailHeaderTitleDataUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailAbiUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.HasInstalledStaticLibrariesUseCase
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailFeatureLoader(
  private val getAppDetailAbiUseCase: GetAppDetailAbiUseCase,
  private val getAppDetailFeaturesUseCase: GetAppDetailFeaturesUseCase,
  private val buildAppDetailAbiLabelDataUseCase: BuildAppDetailAbiLabelDataUseCase,
  private val buildAppDetailHeaderExtraInfoUseCase: BuildAppDetailHeaderExtraInfoUseCase,
  private val buildAppDetailHeaderTitleDataUseCase: BuildAppDetailHeaderTitleDataUseCase,
  private val hasInstalledStaticLibrariesUseCase: HasInstalledStaticLibrariesUseCase
) {
  val featureState = DetailFeatureState()

  fun buildAppDetailAbiLabelData(
    abi: Int,
    abiSet: Collection<Int>,
    apkAnalyticsMode: Boolean
  ): AppDetailAbiLabelData {
    return buildAppDetailAbiLabelDataUseCase(
      abi = abi,
      abiSet = abiSet,
      apkAnalyticsMode = apkAnalyticsMode
    )
  }

  suspend fun buildAppDetailHeaderExtraInfo(
    packageState: DetailPackageState,
    packageInfo: PackageInfo,
    showAndroidVersion: Boolean
  ): AppDetailHeaderExtraInfo {
    return buildAppDetailHeaderExtraInfoUseCase(
      packageInfo = packageInfo,
      apkPreviewInfo = packageState.apkPreviewInfo,
      isApkPreview = packageState.isApkPreview,
      showAndroidVersion = showAndroidVersion
    )
  }

  fun buildAppDetailHeaderTitleData(
    packageState: DetailPackageState,
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = buildAppDetailHeaderTitleDataUseCase(
    packageInfo = packageInfo,
    apkPreviewInfo = packageState.apkPreviewInfo,
    apkAnalyticsMode = apkAnalyticsMode
  )

  suspend fun hasInstalledStaticLibraries(packageName: String): Boolean {
    return hasInstalledStaticLibrariesUseCase(packageName)
  }

  fun emitFeature(
    scope: CoroutineScope,
    feature: VersionedFeature
  ) = scope.launch {
    featureState.emitFeature(feature)
  }

  fun initFeatures(
    scope: CoroutineScope,
    packageState: DetailPackageState,
    packageInfo: PackageInfo,
    features: Int
  ) = scope.launch(Dispatchers.IO) {
    Timber.d("initFeatures: features = $features")

    val detailFeatures = getAppDetailFeaturesUseCase(packageInfo, features, packageState.isApk)
    featureState.emitFeatures(detailFeatures)
  }

  fun initAbiInfo(
    scope: CoroutineScope,
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = scope.launch(Dispatchers.IO) {
    getAppDetailAbiUseCase(packageInfo, apkAnalyticsMode)?.let {
      featureState.emitAbiBundle(it)
    }
  }

  fun initAbiInfo(
    scope: CoroutineScope,
    apkPreviewInfo: ApkPreviewInfo
  ) = scope.launch(Dispatchers.IO) {
    getAppDetailAbiUseCase(apkPreviewInfo)?.let {
      featureState.emitAbiBundle(it)
    }
  }
}
