package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.is16KBAligned
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import java.io.File
import timber.log.Timber

private const val NATIVE_ACTIVITY_CLASS_NAME = "android.app.NativeActivity"
private const val NATIVE_ACTIVITY_LIB_NAME_METADATA = "android.app.lib_name"
private const val NATIVE_ACTIVITY_LABEL = "NativeActivity"
private const val ZYGOTE_PRELOAD_NATIVE_LIB_PROPERTY = "zygotePreloadNativeLib"
private const val ZYGOTE_PRELOAD_NATIVE_LIB_LABEL = "PRELOAD"

class GetAppDetailNativeLibrariesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApk: Boolean,
    isApkPreview: Boolean,
    abi: Int
  ): AppDetailNativeLibraries {
    val specifiedAbi = if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) abi else null
    val parseElf = GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT && !isApkPreview
    val itemsByAbi = if (!isApkPreview && apkPreviewInfo == null) {
      PackageUtils.getSourceLibs(packageInfo, specifiedAbi = specifiedAbi, parseElf = parseElf)
    } else {
      apkPreviewInfo!!.nativeLibs.map {
        ABI_STRING_MAP[it.key]!! to it.value.map { value ->
          LibStringItem(
            name = value.first,
            size = value.second.toLong()
          )
        }
      }.toMap()
    }

    val selectedAbiItems = itemsByAbi[ABI_STRING_MAP[abi % MULTI_ARCH]]
    return AppDetailNativeLibraries(
      itemsByAbi = itemsByAbi,
      selectedAbiSupports16KbPageSize = selectedAbiItems?.let {
        !isApkPreview && packageInfo.is16KBAligned(libs = it, isApk = isApk)
      } ?: false
    )
  }

  suspend fun buildChipList(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean,
    items: List<LibStringItem>,
    sortBySize: Boolean
  ): List<LibStringItemChip> {
    if (items.isEmpty()) {
      return emptyList()
    }

    val packageName = apkPreviewInfo?.packageName ?: packageInfo.packageName
    val nativeActivityLibNames = getNativeActivityLibNames(packageInfo, isApkPreview)
    val preloadNativeLibNames = getZygotePreloadNativeLibNames(packageInfo, apkPreviewInfo)
    val nativeLibNames = items.map { it.name }
    val chipList = items.map {
      val rule = RulesRepository.getRuleWithRegex(it.name, NATIVE, packageName, nativeLibNames)
      val labels = mutableListOf<String>().apply {
        if (it.name in nativeActivityLibNames) {
          add(NATIVE_ACTIVITY_LABEL)
        }
        if (it.name in preloadNativeLibNames) {
          add(ZYGOTE_PRELOAD_NATIVE_LIB_LABEL)
        }
      }
      LibStringItemChip(it, rule, labels)
    }.toMutableList()

    if (sortBySize) {
      chipList.sortByDescending { it.item.size }
    } else {
      chipList.sortWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenByDescending { it.item.size })
    }
    return chipList
  }

  private fun getNativeActivityLibNames(packageInfo: PackageInfo, isApkPreview: Boolean): Set<String> {
    if (isApkPreview) {
      return emptySet()
    }
    val activityPackageInfo = if (packageInfo.activities != null) {
      packageInfo
    } else {
      installedAppRepository.getPackageInfo(
        packageInfo.packageName,
        PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA,
        resolveFrozenArchiveInfo = false
      )
    } ?: return emptySet()

    return activityPackageInfo.activities.orEmpty()
      .asSequence()
      .filter { it.name == NATIVE_ACTIVITY_CLASS_NAME }
      .mapNotNull { it.metaData?.getString(NATIVE_ACTIVITY_LIB_NAME_METADATA) }
      .filter { it.isNotBlank() }
      .map { it.toNativeLibFileName() }
      .toSet()
  }

  private fun getZygotePreloadNativeLibNames(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?
  ): Set<String> {
    if (!OsUtils.atLeastCinnamonBun()) {
      return emptySet()
    }

    val preloadNativeLib = apkPreviewInfo?.appProps?.get(ZYGOTE_PRELOAD_NATIVE_LIB_PROPERTY)
      ?: packageInfo.applicationInfo?.sourceDir?.let { sourceDir ->
        runCatching {
          ApplicationReader.getManifestProperties(File(sourceDir))[ZYGOTE_PRELOAD_NATIVE_LIB_PROPERTY]?.toString()
        }.onFailure {
          Timber.e(it)
        }.getOrNull()
      }
      ?: return emptySet()

    return preloadNativeLib
      .takeIf { it.isNotBlank() }
      ?.let { setOf(it.toNativeLibFileName()) }
      ?: emptySet()
  }

  private fun String.toNativeLibFileName(): String {
    val normalizedName = trim()
      .substringAfterLast('/')
      .removePrefix("lib")
      .removeSuffix(".so")
    return "lib$normalizedName.so"
  }
}

data class AppDetailNativeLibraries(
  val itemsByAbi: Map<String, List<LibStringItem>>,
  val selectedAbiSupports16KbPageSize: Boolean
)
