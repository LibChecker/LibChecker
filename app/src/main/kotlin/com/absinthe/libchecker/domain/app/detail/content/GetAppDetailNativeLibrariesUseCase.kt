package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ET_NOT_SET
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.content.BuildNativeLibraryItemDisplayDataUseCase
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_NATIVE_ACTIVITY_NAMES
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_NATIVE_CHIP_LIST
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_NATIVE_LIBS
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_NATIVE_RULE_MATCH
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_PARSE_SELECTED_ABI
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_PRELOAD_NATIVE_LIB_NAMES
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_SOURCE_LIBS
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_SUPPORTS_16KB
import com.absinthe.libchecker.domain.app.detail.trace.traceDetailSection
import com.absinthe.libchecker.domain.app.detail.trace.traceDetailSuspendSection
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
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
  private val installedAppRepository: InstalledAppRepository,
  private val buildNativeLibraryItemDisplayData: BuildNativeLibraryItemDisplayDataUseCase
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApk: Boolean,
    isApkPreview: Boolean,
    abi: Int
  ): AppDetailNativeLibraries = traceDetailSection(TRACE_DETAIL_NATIVE_LIBS) {
    val specifiedAbi = if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) abi else null
    val parseElf = GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT && !isApkPreview
    val itemsByAbi = traceDetailSection(TRACE_DETAIL_SOURCE_LIBS) {
      if (!isApkPreview && apkPreviewInfo == null) {
        PackageUtils.getSourceLibs(packageInfo, specifiedAbi = specifiedAbi, parseElf = false)
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
    }
    val selectedAbiTab = ABI_STRING_MAP[abi % MULTI_ARCH]
    val resolvedItemsByAbi = if (parseElf && specifiedAbi == null && selectedAbiTab != null) {
      traceDetailSection(TRACE_DETAIL_PARSE_SELECTED_ABI) {
        itemsByAbi.withParsedAbiItems(
          packageInfo = packageInfo,
          tab = selectedAbiTab
        )
      }
    } else {
      itemsByAbi
    }
    val selectedAbiItems = resolvedItemsByAbi[selectedAbiTab]
    AppDetailNativeLibraries(
      itemsByAbi = resolvedItemsByAbi,
      selectedAbiSupports16KbPageSize = traceDetailSection(TRACE_DETAIL_SUPPORTS_16KB) {
        selectedAbiItems?.let {
          !isApkPreview && packageInfo.is16KBAligned(libs = it, isApk = isApk)
        } ?: false
      }
    )
  }

  suspend fun buildChipList(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean,
    tab: String,
    items: List<LibStringItem>,
    sortBySize: Boolean
  ): List<LibStringItemChip> = traceDetailSuspendSection(TRACE_DETAIL_NATIVE_CHIP_LIST) {
    val resolvedItems = resolveNativeLibItemsForTab(
      packageInfo = packageInfo,
      apkPreviewInfo = apkPreviewInfo,
      isApkPreview = isApkPreview,
      tab = tab,
      items = items
    )
    if (resolvedItems.isEmpty()) {
      return@traceDetailSuspendSection emptyList()
    }

    val packageName = apkPreviewInfo?.packageName ?: packageInfo.packageName
    val nativeActivityLibNames = traceDetailSection(TRACE_DETAIL_NATIVE_ACTIVITY_NAMES) {
      getNativeActivityLibNames(packageInfo, isApkPreview)
    }
    val preloadNativeLibNames = traceDetailSection(TRACE_DETAIL_PRELOAD_NATIVE_LIB_NAMES) {
      getZygotePreloadNativeLibNames(packageInfo, apkPreviewInfo)
    }
    val nativeLibNames = resolvedItems.map { it.name }
    val chipList = resolvedItems.map {
      val rule = traceDetailSuspendSection(TRACE_DETAIL_NATIVE_RULE_MATCH) {
        RulesRepository.getRuleWithRegex(it.name, NATIVE, packageName, nativeLibNames)
      }
      val labels = mutableListOf<String>().apply {
        if (it.name in nativeActivityLibNames) {
          add(NATIVE_ACTIVITY_LABEL)
        }
        if (it.name in preloadNativeLibNames) {
          add(ZYGOTE_PRELOAD_NATIVE_LIB_LABEL)
        }
      }
      LibStringItemChip(
        item = it,
        rule = rule,
        labels = labels,
        nativeDisplayData = buildNativeLibraryItemDisplayData(it, labels)
      )
    }.toMutableList()

    if (sortBySize) {
      chipList.sortByDescending { it.item.size }
    } else {
      chipList.sortWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenByDescending { it.item.size })
    }
    chipList
  }

  private fun Map<String, List<LibStringItem>>.withParsedAbiItems(
    packageInfo: PackageInfo,
    tab: String
  ): Map<String, List<LibStringItem>> {
    val abi = STRING_ABI_MAP[tab] ?: return this
    val parsedItems = PackageUtils.getSourceLibs(
      packageInfo = packageInfo,
      specifiedAbi = abi,
      parseElf = true
    )[tab].orEmpty()
    if (parsedItems.isEmpty()) {
      return this
    }
    return toMutableMap().apply {
      put(tab, parsedItems)
    }
  }

  private fun resolveNativeLibItemsForTab(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean,
    tab: String,
    items: List<LibStringItem>
  ): List<LibStringItem> {
    if (
      isApkPreview ||
      apkPreviewInfo != null ||
      GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT.not() ||
      items.none { it.elfInfo.elfType == ET_NOT_SET }
    ) {
      return items
    }

    val abi = STRING_ABI_MAP[tab] ?: return items
    return traceDetailSection(TRACE_DETAIL_PARSE_SELECTED_ABI) {
      PackageUtils.getSourceLibs(
        packageInfo = packageInfo,
        specifiedAbi = abi,
        parseElf = true
      )[tab]?.takeIf { it.isNotEmpty() } ?: items
    }
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
