package com.absinthe.libchecker.domain.app.detail.content

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.ET_DYN
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ET_NOT_SET
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.EXPORTED
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.model.NativeLibraryItemDisplayData
import com.absinthe.libchecker.domain.app.detail.model.StatefulComponent
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
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.ParsedIntentFilter
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_16_KB
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import com.absinthe.libchecker.utils.extensions.getSignatures
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList
import com.absinthe.libchecker.utils.extensions.is16KBAligned
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import com.absinthe.rulesbundle.Rule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager
import timber.log.Timber

private const val NATIVE_ACTIVITY_CLASS_NAME = "android.app.NativeActivity"
private const val NATIVE_ACTIVITY_LIB_NAME_METADATA = "android.app.lib_name"
private const val NATIVE_ACTIVITY_LABEL = "NativeActivity"
private const val ZYGOTE_PRELOAD_NATIVE_LIB_PROPERTY = "zygotePreloadNativeLib"
private const val ZYGOTE_PRELOAD_NATIVE_LIB_LABEL = "PRELOAD"
private const val LIVE_UPDATE_NOTIFICATION_PERMISSION = "android.permission.POST_PROMOTED_NOTIFICATIONS"

class DetailContentResolver(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository
) {

  fun getNativeLibraries(
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

  suspend fun getComponentChips(
    packageInfo: PackageInfo,
    isApk: Boolean
  ): AppDetailComponentChips {
    return getComponents(packageInfo, isApk).toChips(
      packageName = packageInfo.packageName,
      useIntentFilterRules = true
    )
  }

  suspend fun getComponentChips(previewInfo: ApkPreviewInfo): AppDetailComponentChips {
    return getComponents(previewInfo).toChips(
      packageName = previewInfo.packageName,
      useIntentFilterRules = false
    )
  }

  private fun getComponents(
    packageInfo: PackageInfo,
    isApk: Boolean
  ): AppDetailComponents {
    val parsedIntentFiltersByClassName = packageInfo.applicationInfo?.sourceDir
      ?.let { sourceDir ->
        IntentFilterUtils.parseComponentsFromApk(sourceDir)
          .asSequence()
          .map { item -> item.className to item.intentFilters }
          .toMap()
      }
      .orEmpty()

    return AppDetailComponents(
      services = packageInfo.getComponents(isApk, SERVICE),
      activities = packageInfo.getComponents(isApk, ACTIVITY),
      receivers = packageInfo.getComponents(isApk, RECEIVER),
      providers = packageInfo.getComponents(isApk, PROVIDER),
      intentFiltersByClassName = parsedIntentFiltersByClassName
    )
  }

  private fun getComponents(previewInfo: ApkPreviewInfo): AppDetailComponents {
    return AppDetailComponents(
      services = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.services, true),
      activities = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.activities, true),
      receivers = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.receivers, true),
      providers = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.providers, true)
    )
  }

  private fun PackageInfo.getComponents(
    isApk: Boolean,
    @LibType type: Int
  ): List<StatefulComponent> {
    val components = componentInfoList(type)
    val source = if (components?.isNotEmpty() == true || isApk) {
      components
    } else {
      // Do not combine component flags here: huge apps can exceed Binder's
      // transaction limit when a single PackageInfo carries every component.
      installedAppRepository.getPackageInfo(packageName, componentFlag(type))
        ?.componentInfoList(type)
    }

    return PackageUtils.getComponentList(packageName, source, true)
  }

  private fun PackageInfo.componentInfoList(@LibType type: Int): Array<out ComponentInfo>? {
    return when (type) {
      SERVICE -> services
      ACTIVITY -> activities
      RECEIVER -> receivers
      PROVIDER -> providers
      else -> null
    }
  }

  private fun componentFlag(@LibType type: Int): Int {
    return when (type) {
      SERVICE -> PackageManager.GET_SERVICES
      ACTIVITY -> PackageManager.GET_ACTIVITIES
      RECEIVER -> PackageManager.GET_RECEIVERS
      PROVIDER -> PackageManager.GET_PROVIDERS
      else -> 0
    }
  }

  private suspend fun AppDetailComponents.toChips(
    packageName: String,
    useIntentFilterRules: Boolean
  ): AppDetailComponentChips {
    val ruleCache = mutableMapOf<String, Rule?>()

    suspend fun getRuleCached(name: String, @LibType type: Int, regex: Boolean): Rule? {
      val key = "$type:$regex:$name"
      if (ruleCache.containsKey(key)) {
        return ruleCache[key]
      }
      return RulesRepository.getRule(name, type, regex).also {
        ruleCache[key] = it
      }
    }

    suspend fun StatefulComponent.toChip(@LibType componentType: Int): LibStringItemChip {
      var rule = if (!componentName.startsWith(".")) {
        getRuleCached(componentName, componentType, true)
      } else {
        null
      }
      if (rule == null && useIntentFilterRules) {
        val fullComponentName = if (componentName.startsWith(".")) {
          packageName + componentName
        } else {
          componentName
        }
        intentFiltersByClassName[fullComponentName]?.let { filters ->
          for (filter in filters) {
            for (action in filter.actions) {
              rule = getRuleCached(action, ACTION_IN_RULES, false)
              if (rule != null) break
            }
            if (rule != null) break
          }
        }
      }

      val source = when {
        !enabled -> DISABLED
        exported -> EXPORTED
        else -> null
      }

      return LibStringItemChip(
        LibStringItem(
          name = componentName,
          source = source,
          process = processName.takeIf { it.isNotEmpty() }
        ),
        rule
      )
    }

    return AppDetailComponentChips(
      services = services.map { it.toChip(SERVICE) },
      activities = activities.map { it.toChip(ACTIVITY) },
      receivers = receivers.map { it.toChip(RECEIVER) },
      providers = providers.map { it.toChip(PROVIDER) },
      processNames = sequenceOf(services, activities, receivers, providers)
        .flatten()
        .map { it.processName }
        .filter { it.isNotEmpty() }
        .toSet()
    )
  }

  fun getAbilityChips(packageName: String): Map<Int, List<LibStringItemChip>> {
    val abilities = ApplicationDelegate(context).iBundleManager?.getBundleInfo(
      packageName,
      IBundleManager.GET_BUNDLE_WITH_ABILITIES
    )?.abilityInfos ?: return emptyMap()

    return mapOf(
      AbilityType.PAGE to abilities.asSequence().toAbilityChips(AbilityInfo.AbilityType.PAGE),
      AbilityType.SERVICE to abilities.asSequence().toAbilityChips(AbilityInfo.AbilityType.SERVICE),
      AbilityType.WEB to abilities.asSequence().toAbilityChips(AbilityInfo.AbilityType.WEB),
      AbilityType.DATA to abilities.asSequence().toAbilityChips(AbilityInfo.AbilityType.DATA)
    )
  }

  private fun Sequence<AbilityInfo>.toAbilityChips(
    abilityType: AbilityInfo.AbilityType
  ): List<LibStringItemChip> {
    return filter { it.type == abilityType }
      .map { ability ->
        LibStringItemChip(
          LibStringItem(
            name = ability.className,
            source = DISABLED.takeIf { !ability.enabled }
          ),
          null
        )
      }
      .toList()
  }

  suspend fun getDexChips(
    packageInfo: PackageInfo,
    sortBySizeMode: Boolean
  ): List<LibStringItemChip> {
    Timber.d("getDexChipList")
    val items = runCatching {
      PackageUtils.getDexList(packageInfo)
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(emptyList())
    if (items.isEmpty()) {
      return emptyList()
    }

    return items.map {
      LibStringItemChip(it, RulesRepository.getRule(it.name, DEX, true))
    }.sortedWith(
      if (sortBySizeMode) {
        compareByDescending { it.item.name }
      } else {
        compareByDescending { it.rule != null }
      }
    )
  }

  fun getMetadataChips(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean
  ): List<LibStringItemChip> {
    val items = if (!isApkPreview && apkPreviewInfo == null) {
      getInstalledMetadataChips(packageInfo)
    } else {
      apkPreviewInfo!!.metadata
        .map { metadata ->
          var flag = 0L
          val value = if (metadata.value is Long || (metadata.value as? String)?.maybeResourceId() == true) {
            flag = -1
            null
          } else {
            metadata.value.toString()
          }
          LibStringItemChip(
            LibStringItem(metadata.key, flag, value),
            null
          )
        }
    }.toMutableList()

    items.sortByDescending { it.item.name }
    return items
  }

  private fun getInstalledMetadataChips(packageInfo: PackageInfo): List<LibStringItemChip> {
    val applicationInfo = packageInfo.applicationInfo ?: return emptyList()
    val metadata = applicationInfo.metaData ?: return emptyList()
    val appResources = runCatching {
      context.packageManager.getResourcesForApplication(applicationInfo)
    }.getOrNull()

    return metadata.keySet().asSequence()
      .map { key ->
        @Suppress("DEPRECATION")
        val value = metadata.get(key).toString()
        val resourceId = value.toResourceIdOrNull()
        val displayValue = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceName(id)
          }.getOrNull()
        } ?: value
        val resourceType = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceTypeName(id)
          }.getOrNull()
        }

        LibStringItemChip(
          LibStringItem(key, resourceId?.toLong() ?: 0L, displayValue),
          null,
          listOfNotNull(resourceType)
        )
      }
      .toList()
  }

  private fun String.toResourceIdOrNull(): Int? {
    return takeIf(String::maybeResourceId)
      ?.toLongOrNull()
      ?.takeIf { id -> id in Int.MIN_VALUE..Int.MAX_VALUE }
      ?.toInt()
      ?.takeIf { id -> id ushr 24 != 0 }
  }

  fun getPermissionChips(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApk: Boolean,
    isApkPreview: Boolean
  ): AppDetailPermissionChips {
    val items = if (!isApkPreview && apkPreviewInfo == null) {
      packageInfo.getStatefulPermissionsList().asSequence()
        .map { permission ->
          val granted = permission.second && !isApk
          LibStringItemChip(
            LibStringItem(
              name = permission.first,
              size = if (granted) PackageInfo.REQUESTED_PERMISSION_GRANTED.toLong() else 0,
              source = if (permission.first.contains("maxSdkVersion")) DISABLED else null,
              process = if (granted) PackageInfo.REQUESTED_PERMISSION_GRANTED.toString() else null
            ),
            null
          )
        }
    } else {
      apkPreviewInfo!!.permissions.asSequence()
        .map { permission ->
          LibStringItemChip(
            LibStringItem(name = permission, size = 0, source = null, process = null),
            null
          )
        }
    }.toMutableList()

    items.sortByDescending { it.item.name }
    return AppDetailPermissionChips(
      items = items,
      hasLiveUpdateNotification = items.any { it.item.name == LIVE_UPDATE_NOTIFICATION_PERMISSION }
    )
  }

  suspend fun getSignatureChips(
    packageInfo: PackageInfo,
    isApk: Boolean
  ): List<LibStringItemChip> = withContext(Dispatchers.IO) {
    runCatching {
      @Suppress("InlinedApi", "DEPRECATION")
      val flags = PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES
      if (!isApk) {
        installedAppRepository.getPackageInfo(packageInfo.packageName, flags)
          ?.getSignatures(context)
          ?: emptySequence()
      } else {
        PackageManagerCompat.getPackageArchiveInfo(packageInfo.applicationInfo!!.sourceDir, flags)!!
          .getSignatures(context)
      }
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(emptySequence())
      .map {
        LibStringItemChip(it, null)
      }.toList()
  }

  suspend fun getStaticLibraryChips(
    packageInfo: PackageInfo,
    sortBySizeMode: Boolean
  ): List<LibStringItemChip> {
    val items = runCatching { PackageUtils.getStaticLibs(packageInfo) }.getOrDefault(emptyList())
    if (items.isEmpty()) {
      return emptyList()
    }

    return items.map {
      LibStringItemChip(it, RulesRepository.getRule(it.name, STATIC, false))
    }.sortedWith(
      if (sortBySizeMode) {
        compareByDescending { it.item.name }
      } else {
        compareByDescending { it.rule != null }
      }
    )
  }

  suspend fun getStaticLibraryTabItems(
    packageInfo: PackageInfo,
    packageName: String,
    sortBySizeMode: Boolean
  ): List<LibStringItemChip> = withContext(Dispatchers.IO) {
    val sharedLibraryFiles = packageInfo.applicationInfo?.sharedLibraryFiles
    if (sharedLibraryFiles?.isNotEmpty() != true) {
      return@withContext emptyList()
    }

    val installedPackageInfo = installedAppRepository.getPackageInfo(packageName)
      ?: return@withContext emptyList()
    getStaticLibraryChips(installedPackageInfo, sortBySizeMode)
  }

  private fun buildNativeLibraryItemDisplayData(
    item: LibStringItem,
    labels: List<String>
  ): NativeLibraryItemDisplayData {
    val elfInfo = item.elfInfo
    return NativeLibraryItemDisplayData(
      sizeText = PackageUtils.sizeToString(context, item),
      labels = buildList {
        if (elfInfo.elfType != ET_NOT_SET && elfInfo.elfType != ET_DYN) {
          add(PackageUtils.elfTypeToString(elfInfo.elfType))
        }
        if (elfInfo.elfType != ET_NOT_ELF) {
          if (elfInfo.pageSize > 0 && elfInfo.pageSize % PAGE_SIZE_16_KB == 0) {
            add("16 KB")
          }
          getZipAlignmentText(elfInfo.zipAlignment)?.let(::add)
        }
        addAll(labels)
      }
    )
  }

  private fun getZipAlignmentText(zipAlignment: Long): String? {
    if (zipAlignment <= 0L || zipAlignment >= PAGE_SIZE_16_KB) {
      return null
    }
    return if (zipAlignment >= 1024L && zipAlignment % 1024L == 0L) {
      "${zipAlignment / 1024}KB ZIPALIGN"
    } else {
      "${zipAlignment}B ZIPALIGN"
    }
  }
}

data class AppDetailNativeLibraries(
  val itemsByAbi: Map<String, List<LibStringItem>>,
  val selectedAbiSupports16KbPageSize: Boolean
)

data class AppDetailComponents(
  val services: List<StatefulComponent>,
  val activities: List<StatefulComponent>,
  val receivers: List<StatefulComponent>,
  val providers: List<StatefulComponent>,
  val intentFiltersByClassName: Map<String, List<ParsedIntentFilter>> = emptyMap()
)

data class AppDetailComponentChips(
  val services: List<LibStringItemChip>,
  val activities: List<LibStringItemChip>,
  val receivers: List<LibStringItemChip>,
  val providers: List<LibStringItemChip>,
  val processNames: Set<String>
)

data class AppDetailPermissionChips(
  val items: List<LibStringItemChip>,
  val hasLiveUpdateNotification: Boolean
)
