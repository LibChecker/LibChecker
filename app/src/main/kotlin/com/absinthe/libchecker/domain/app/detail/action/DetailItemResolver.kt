package com.absinthe.libchecker.domain.app.detail.action

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailContentDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailLocaleDisplay
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailContent
import com.absinthe.libchecker.domain.app.detail.resource.AppResourceReference
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.app.repository.LibraryDetailRepository
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.elf.ElfParser
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import com.absinthe.libchecker.utils.manifest.PropertiesMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import pxb.android.axml.ValueWrapper
import timber.log.Timber

class DetailItemResolver(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository,
  private val libraryDetailRepository: LibraryDetailRepository
) {

  suspend fun getHeader(request: HeaderRequest): LibraryDetailHeaderDisplay = withContext(Dispatchers.IO) {
    val rule = if (request.isValidLib) {
      RulesRepository.getRule(request.libName, request.type, true)
    } else {
      null
    }
    LibraryDetailHeaderDisplay(
      iconRes = rule?.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder,
      isSimpleColorIcon = rule?.isSimpleColorIcon == true
    )
  }

  suspend operator fun invoke(request: Request): Result = withContext(Dispatchers.IO) {
    if (!request.isValidLib) {
      return@withContext Result.NotFound
    }

    val regexName = request.regexName?.takeIf { it.isNotEmpty() }
    val detail = getLibraryDetail(
      libName = regexName ?: request.libName,
      type = request.type,
      isRegex = regexName != null
    ) ?: return@withContext Result.NotFound

    val content = buildLibraryDetailContentDisplay(
      detail = detail,
      repoUpdatedTime = detail.getRepoUpdatedTime(),
      preferredLocale = request.preferredLocale
    ) ?: return@withContext Result.NotFound

    Result.Found(content)
  }

  private suspend fun LibDetailBean.getRepoUpdatedTime(): String? {
    if (!GlobalValues.isGitHubReachable &&
      GlobalValues.githubApiAuthorizationHeaderFor(ApiManager.GITHUB_API_REPO_INFO) == null
    ) {
      return null
    }

    val sourceLink = data.firstOrNull()?.data?.source_link.orEmpty()
    if (!sourceLink.startsWith(URLManager.GITHUB_HOST)) {
      return null
    }

    val splits = sourceLink.removePrefix(URLManager.GITHUB_HOST).split("/")
    if (splits.size < 2) {
      return null
    }
    return getRepoUpdatedTime(splits[0], splits[1])
  }

  private suspend fun getLibraryDetail(
    libName: String,
    @LibType type: Int,
    isRegex: Boolean
  ): LibDetailBean? {
    val categoryDir = type.toCategoryDir(isRegex)
    val libPath = if (type in listOf(SERVICE, ACTIVITY, RECEIVER, PROVIDER, STATIC)) {
      libName.replace(".", "/")
    } else {
      libName
    }
    Timber.d("requestLibDetail: categoryDir = $categoryDir, libPath = $libPath")

    return runCatching {
      libraryDetailRepository.requestLibraryDetail(categoryDir, libPath)
    }.onFailure {
      Timber.w(it, "Failed to request lib detail: $categoryDir/$libPath")
    }.getOrNull()
  }

  private suspend fun getRepoUpdatedTime(owner: String, repo: String): String? {
    val pushedAt = DateUtils.parseIso8601DateTime(
      libraryDetailRepository.getRepoPushedAt(owner, repo) ?: return null
    ) ?: return null
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(pushedAt)
  }

  private fun Int.toCategoryDir(isRegex: Boolean): String {
    var categoryDir = when (this) {
      NATIVE -> "native-libs"
      SERVICE -> "services-libs"
      ACTIVITY -> "activities-libs"
      RECEIVER -> "receivers-libs"
      PROVIDER -> "providers-libs"
      DEX -> "dex-libs"
      STATIC -> "static-libs"
      ACTION -> "actions-libs"
      else -> throw IllegalArgumentException("Illegal LibType: $this.")
    }
    if (isRegex) {
      categoryDir += "/regex"
    }
    return categoryDir
  }

  suspend fun getAppManifestProperties(
    packageInfo: PackageInfo?,
    properties: Map<String, *>? = null
  ): List<AppPropItem> = withContext(Dispatchers.IO) {
    val propertyMap = properties ?: packageInfo?.applicationInfo?.sourceDir
      ?.let { sourceDir ->
        runCatching {
          ApplicationReader.getManifestProperties(File(sourceDir))
        }.getOrNull()
      }
    val appResources = packageInfo?.applicationInfo?.let { applicationInfo ->
      runCatching {
        packageManager.getResourcesForApplication(applicationInfo)
      }.getOrNull()
    }

    propertyMap.orEmpty()
      .map { property ->
        val value = property.value.toPropertyValue()
        val resourceId = value.toResourceIdOrNull()
        val resourceName = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceName(id)
          }.getOrNull()
        }
        val resourceType = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceTypeName(id)
          }.getOrNull()
        }
        AppPropItem(
          key = property.key,
          originalDisplayValue = resourceName ?: PropertiesMap.parseProperty(property.key, value),
          resource = AppResourceReference.create(resourceId, resourceType)
        )
      }
      .sortedBy(AppPropItem::key)
  }

  suspend fun getElfDetail(
    packageName: String,
    elfPath: String
  ): AppElfDetail? = withContext(Dispatchers.IO) {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return@withContext null
    val nativePath = packageInfo.applicationInfo?.nativeLibraryDir
    if (nativePath != null) {
      File(nativePath).listFiles()
        ?.find { it.path.endsWith(elfPath) }
        ?.let { return@withContext it.readElfDetail() }
    }

    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return@withContext null
    findElfDetailInApk(File(sourceDir), elfPath)?.let { return@withContext it }

    PackageUtils.getSplitsSourceDir(packageInfo)?.forEach { split ->
      findElfDetailInApk(File(split), elfPath)?.let { return@withContext it }
    }
    null
  }

  suspend fun getPermissionDetail(permissionName: String): PermissionDetailContent = withContext(Dispatchers.IO) {
    val normalizedName = permissionName.substringBefore(" ")
    val permissionInfo = runCatching {
      packageManager.getPermissionInfo(normalizedName, 0)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()

    PermissionDetailContent(
      name = normalizedName,
      icon = permissionInfo?.loadIconOrNull(),
      label = permissionInfo?.loadLabelOrNull(),
      description = permissionInfo?.loadDescriptionOrNull(),
      providerAppName = permissionInfo?.packageName?.let(::getProviderAppName)
    )
  }

  suspend fun getPermissionProviders(permissionName: String): List<ProviderPermissionItem> {
    val result = mutableListOf<ProviderPermissionItem>()
    val seenPackages = mutableSetOf<String>()
    val coroutineContext = currentCoroutineContext()

    for (app in installedAppRepository.getApplicationList()) {
      if (!coroutineContext.isActive) {
        return result
      }

      val packageInfo = runCatching {
        installedAppRepository.getPackageInfo(
          app.packageName,
          PackageManager.GET_PROVIDERS
        )
      }.onFailure {
        Timber.w(it, "Failed to get package info for ${app.packageName}")
      }.getOrNull() ?: continue

      val providers = packageInfo.providers ?: continue
      for (provider in providers) {
        if (provider.readPermission == permissionName ||
          provider.writePermission == permissionName
        ) {
          if (seenPackages.add(app.packageName)) {
            result.add(
              ProviderPermissionItem(
                packageName = app.packageName,
                providerName = provider.name
              )
            )
          }
          break
        }
      }
    }

    return result
  }

  private fun Any?.toPropertyValue(): String {
    return when (this) {
      is ValueWrapper -> ref.toString()
      else -> this?.toString().orEmpty()
    }
  }

  private fun String.toResourceIdOrNull(): Int? {
    return takeIf(String::maybeResourceId)?.toIntOrNull()
  }

  private fun File.readElfDetail(): AppElfDetail {
    return ElfParser(this).readElfDetail()
  }

  private fun findElfDetailInApk(apk: File, elfPath: String): AppElfDetail? {
    ZipFileCompat(apk).use { zipFile ->
      val entry = zipFile.getEntry(elfPath) ?: return null
      return ElfParser(zipFile.getInputStream(entry)).readElfDetail()
    }
  }

  private fun ElfParser.readElfDetail(): AppElfDetail {
    return use {
      AppElfDetail(
        deps = parseNeededDependencies(),
        entryPoints = parseEntryPoints(),
        isStripped = isSymbolTableStripped()
      )
    }
  }

  private fun PermissionInfo.loadIconOrNull(): Drawable? {
    if (icon == 0) {
      return null
    }
    return runCatching {
      loadIcon(packageManager)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private fun PermissionInfo.loadLabelOrNull(): CharSequence? {
    return runCatching {
      loadLabel(packageManager).takeIf(CharSequence::isNotEmpty)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private fun PermissionInfo.loadDescriptionOrNull(): CharSequence? {
    return runCatching {
      loadDescription(packageManager)?.takeIf(CharSequence::isNotEmpty)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private fun getProviderAppName(packageName: String): String? {
    return installedAppRepository.getPackageInfo(packageName)?.getAppName(packageManager)
  }

  data class HeaderRequest(
    val libName: String,
    @LibType val type: Int,
    val isValidLib: Boolean
  )

  data class Request(
    val libName: String,
    @LibType val type: Int,
    val regexName: String?,
    val isValidLib: Boolean,
    val preferredLocale: String
  )

  sealed interface Result {
    data object NotFound : Result

    data class Found(
      val content: LibraryDetailContentDisplay
    ) : Result
  }
}

data class AppElfDetail(
  val deps: List<String>,
  val entryPoints: List<String>,
  val isStripped: Boolean
)

data class ProviderPermissionItem(
  val packageName: String,
  val providerName: String
)

internal fun buildLibraryDetailContentDisplay(
  detail: LibDetailBean,
  repoUpdatedTime: String?,
  preferredLocale: String,
  localeNameResolver: (String) -> String = { Locale.forLanguageTag(it).displayName }
): LibraryDetailContentDisplay? {
  val locales = detail.data.map { localizedDetail ->
    val data = localizedDetail.data
    LibraryDetailLocaleDisplay(
      localeTag = localizedDetail.locale,
      localeName = localeNameResolver(localizedDetail.locale),
      items = buildList {
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_label,
            tipRes = R.string.lib_detail_label_tip,
            textStyle = DetailInfoTextStyle.TITLE,
            text = data.label
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_team,
            tipRes = R.string.lib_detail_develop_team_tip,
            textStyle = DetailInfoTextStyle.TITLE,
            text = data.dev_team
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_github,
            tipRes = R.string.lib_detail_rule_contributors_tip,
            textStyle = DetailInfoTextStyle.TITLE,
            text = data.rule_contributors.joinToString(separator = ", ")
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_content,
            tipRes = R.string.lib_detail_description_tip,
            textStyle = DetailInfoTextStyle.BODY,
            text = data.description
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_url,
            tipRes = R.string.lib_detail_relative_link_tip,
            textStyle = DetailInfoTextStyle.BODY,
            text = data.source_link,
            linkUrl = data.source_link
          )
        )
        repoUpdatedTime?.let { updatedAt ->
          add(
            DetailInfoItemDisplay(
              iconRes = R.drawable.ic_time,
              tipRes = R.string.lib_detail_last_update_tip,
              textStyle = DetailInfoTextStyle.BODY,
              text = updatedAt
            )
          )
        }
      }
    )
  }
  if (locales.isEmpty()) {
    return null
  }

  val selectedLocaleTag = locales
    .firstOrNull { it.localeTag == preferredLocale }
    ?.localeTag
    ?: locales.first().localeTag
  return LibraryDetailContentDisplay(
    locales = locales,
    selectedLocaleTag = selectedLocaleTag
  )
}
