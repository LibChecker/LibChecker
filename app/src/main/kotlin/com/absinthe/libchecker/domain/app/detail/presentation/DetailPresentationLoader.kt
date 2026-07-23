package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.Context
import android.content.pm.PackageInfo
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbi
import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbiLabel
import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemRequest
import com.absinthe.libchecker.domain.app.detail.feature.BuildAppDetailFeatureItemUseCase
import com.absinthe.libchecker.domain.app.detail.feature.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderTitleData
import com.absinthe.libchecker.domain.app.detail.packageinfo.AppDetailPackageSize
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageSizeUseCase
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersionString
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.getVersionString
import com.absinthe.libchecker.utils.extensions.sizeToString
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class DetailPresentationLoader(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository,
  private val getAppDetailPackageSize: GetAppDetailPackageSizeUseCase,
  private val getAppDetailFeaturesUseCase: GetAppDetailFeaturesUseCase,
  private val buildAppDetailFeatureItemUseCase: BuildAppDetailFeatureItemUseCase
) {
  val featureState = DetailFeatureState()

  fun reset() {
    featureState.reset()
  }

  fun buildAppDetailAbiLabelData(
    abi: Int,
    abiSet: Collection<Int>,
    apkAnalyticsMode: Boolean
  ): AppDetailAbiLabelData {
    val trueAbi = abi.mod(Constants.MULTI_ARCH)
    if (abiSet.isEmpty() || abiSet.contains(Constants.OVERLAY) || abiSet.contains(Constants.ERROR)) {
      return AppDetailAbiLabelData(
        is64Bit = PackageUtils.isAbi64Bit(trueAbi),
        labels = emptyList()
      )
    }
    val labels = buildList {
      if (abi >= Constants.MULTI_ARCH) {
        add(buildAbiLabel(Constants.MULTI_ARCH, isActive = true, opensMultiArchInfo = true))
      }
      abiSet.filterNot { it == Constants.NO_LIBS }.forEach {
        add(buildAbiLabel(it, isActive = apkAnalyticsMode || it == trueAbi))
      }
    }
    return AppDetailAbiLabelData(
      is64Bit = PackageUtils.isAbi64Bit(trueAbi),
      labels = labels
    )
  }

  suspend fun buildAppDetailHeaderExtraInfo(
    packageState: DetailPackageState,
    packageInfo: PackageInfo,
    showAndroidVersion: Boolean
  ): AppDetailHeaderExtraInfo = withContext(Dispatchers.IO) {
    val apkPreviewInfo = packageState.apkPreviewInfo
    val applicationInfo = packageInfo.applicationInfo!!
    val targetSdkVersion = apkPreviewInfo?.targetSdkVersion ?: applicationInfo.targetSdkVersion
    val minSdkVersion = apkPreviewInfo?.minSdkVersion ?: applicationInfo.minSdkVersion
    val compileSdkVersion = apkPreviewInfo?.compileSdkVersion ?: packageInfo.getCompileSdkVersion()
    val packageSize = getAppDetailPackageSize(packageInfo, apkPreviewInfo, packageState.isApkPreview)
    AppDetailHeaderExtraInfo(
      targetSdkInfo = formatSdkInfo(
        apkPreviewInfo?.targetSdkVersion?.toString() ?: packageInfo.getTargetApiString(),
        targetSdkVersion,
        showAndroidVersion
      ),
      minSdkInfo = formatSdkInfo(minSdkVersion.toString(), minSdkVersion, showAndroidVersion),
      compileSdkInfo = formatSdkInfo(
        apkPreviewInfo?.compileSdkVersion?.toString() ?: packageInfo.getCompileSdkVersionString(),
        compileSdkVersion,
        showAndroidVersion
      ),
      sizeInfo = formatPackageSize(packageSize),
      sharedUserId = packageInfo.sharedUserId
    )
  }

  fun buildAppDetailHeaderTitleData(
    packageState: DetailPackageState,
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ): AppDetailHeaderTitleData {
    val apkPreviewInfo = packageState.apkPreviewInfo
    val packageName = apkPreviewInfo?.packageName ?: packageInfo.packageName
    val appName = apkPreviewInfo
      ?.let { context.getString(R.string.apk_preview) }
      ?: packageInfo.getAppName(context.packageManager)
    return AppDetailHeaderTitleData(
      packageName = packageName,
      appName = appName,
      title = appName ?: context.getString(R.string.detail_label),
      versionInfo = apkPreviewInfo
        ?.let { "${it.versionName} (${it.versionCode})" }
        ?: packageInfo.getVersionString(),
      isAppInfoAvailable = !apkAnalyticsMode || installedAppRepository.isPackageInstalled(packageName)
    )
  }

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

  fun emitFeature(
    scope: CoroutineScope,
    feature: VersionedFeature
  ) = scope.launch {
    featureState.emitFeature(feature)
  }

  fun setLoading(
    scope: CoroutineScope,
    loading: Boolean
  ) = scope.launch {
    featureState.setLoading(loading)
  }

  fun initFeatures(
    scope: CoroutineScope,
    packageState: DetailPackageState,
    packageInfo: PackageInfo,
    features: Int
  ) = scope.launch(Dispatchers.IO) {
    featureState.setLoading(true)
    try {
      Timber.d("initFeatures: features = $features")

      getAppDetailFeaturesUseCase(
        packageInfo = packageInfo,
        cachedFeatures = features,
        isApk = packageState.isApk,
        onFeature = featureState::emitFeature,
        onAppIcons = featureState::setAppIcons
      )
    } finally {
      featureState.setLoading(false)
    }
  }

  fun initAbiInfo(
    scope: CoroutineScope,
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = scope.launch(Dispatchers.IO) {
    getAppDetailAbi(packageInfo, apkAnalyticsMode)?.let {
      featureState.emitAbiBundle(it)
    }
  }

  fun initAbiInfo(
    scope: CoroutineScope,
    apkPreviewInfo: ApkPreviewInfo
  ) = scope.launch(Dispatchers.IO) {
    getAppDetailAbi(apkPreviewInfo)?.let {
      featureState.emitAbiBundle(it)
    }
  }

  private fun buildAbiLabel(
    abi: Int,
    isActive: Boolean,
    opensMultiArchInfo: Boolean = false
  ): AppDetailAbiLabel {
    return AppDetailAbiLabel(
      abi = abi,
      isActive = isActive,
      contentDescription = if (abi == Constants.MULTI_ARCH) {
        context.getString(R.string.multiArch)
      } else {
        PackageUtils.getAbiString(context, abi, showExtraInfo = false)
      },
      is64Bit = PackageUtils.isAbi64Bit(abi),
      opensMultiArchInfo = opensMultiArchInfo
    )
  }

  private fun getAppDetailAbi(packageInfo: PackageInfo, isApk: Boolean): AppDetailAbi? {
    val source = runCatching { packageInfo.applicationInfo?.sourceDir }.getOrNull() ?: return null
    val abiSet = PackageUtils.getAbiSet(
      file = File(source),
      packageInfo = packageInfo,
      isApk = isApk,
      ignoreArch = true
    ).toSet()
    return buildAppDetailAbi(PackageUtils.getAbi(packageInfo, isApk = isApk, abiSet = abiSet), abiSet)
  }

  private fun getAppDetailAbi(apkPreviewInfo: ApkPreviewInfo): AppDetailAbi? {
    val abiSet = apkPreviewInfo.abiSet
    return abiSet.firstOrNull()?.let { buildAppDetailAbi(it, abiSet) }
  }

  private fun buildAppDetailAbi(abi: Int, abiSet: Collection<Int>): AppDetailAbi {
    return AppDetailAbi(
      abi = abi,
      abiSet = abiSet.sortedByDescending { it == abi || PackageUtils.isAbi64Bit(it) }
    )
  }

  private fun formatSdkInfo(value: String, version: Int, showAndroidVersion: Boolean): String {
    return if (showAndroidVersion) "$value (${AndroidVersions.simpleVersions[version]})" else value
  }

  private fun formatPackageSize(packageSize: AppDetailPackageSize): String {
    val sizes = buildList {
      add(packageSize.baseSize.sizeToString(context, showBytes = false))
      packageSize.splitSizes.mapTo(this) { it.sizeToString(context, showBytes = false) }
    }
    if (sizes.size == 1) return sizes.single()
    val totalSize = packageSize.totalSize.sizeToString(context, showBytes = false)
    return sizes.joinToString(separator = " + ", prefix = "(", postfix = " = $totalSize)")
  }
}
