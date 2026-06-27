package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.detail.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.detail.BuildAppDetailAbiLabelDataUseCase
import com.absinthe.libchecker.domain.app.detail.BuildAppDetailHeaderExtraInfoUseCase
import com.absinthe.libchecker.domain.app.detail.BuildAppDetailHeaderTitleDataUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailAbiUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.detail.ShouldShowStaticLibraryTabUseCase
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemRequest
import com.absinthe.libchecker.domain.app.detail.feature.BuildAppDetailFeatureItemUseCase
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
  private val buildAppDetailFeatureItemUseCase: BuildAppDetailFeatureItemUseCase,
  private val shouldShowStaticLibraryTabUseCase: ShouldShowStaticLibraryTabUseCase
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

  fun buildAppDetailFeatureItem(
    feature: VersionedFeature,
    currentFeatureCount: Int,
    apkAnalyticsMode: Boolean,
    canShowInstallSource: Boolean,
    canShowAppIcons: Boolean
  ) = buildAppDetailFeatureItemUseCase(
    AppDetailFeatureItemRequest(
      feature = feature,
      currentFeatureCount = currentFeatureCount,
      apkAnalyticsMode = apkAnalyticsMode,
      canShowInstallSource = canShowInstallSource,
      canShowAppIcons = canShowAppIcons,
      appIcons = featureState.appIcons
    )
  )

  suspend fun shouldShowStaticLibraryTab(
    packageInfo: PackageInfo,
    packageName: String
  ): Boolean {
    return shouldShowStaticLibraryTabUseCase(packageInfo, packageName)
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
