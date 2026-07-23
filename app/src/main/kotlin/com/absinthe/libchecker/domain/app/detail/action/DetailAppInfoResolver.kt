package com.absinthe.libchecker.domain.app.detail.action

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchItem
import com.absinthe.libchecker.domain.app.detail.model.AppInfoActionItem
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailExtraInfoDisplay
import com.absinthe.libchecker.domain.app.detail.model.OverlayTargetPackageDisplay
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData
import com.absinthe.libchecker.domain.app.list.related.RelatedAppListItem
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.PREINSTALLED_TIMESTAMP
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersionString
import com.absinthe.libchecker.utils.extensions.getDexFileOptimizationInfo
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.getVersionString
import com.absinthe.libchecker.utils.extensions.sizeToString
import dev.rikka.tools.refine.Refine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetailAppInfoResolver(
  private val context: Context,
  private val ownPackageName: String,
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val allowFileUriExposure: AllowFileUriExposureUseCase
) {
  private val packageManager = context.packageManager

  suspend fun getXposedModuleInfo(packageName: String): XposedModuleInfo? = withContext(Dispatchers.IO) {
    val packageInfo = installedAppRepository.getPackageInfo(packageName, PackageManager.GET_META_DATA)
      ?: return@withContext null
    val applicationInfo = packageInfo.applicationInfo
    val metadataBundle = PackageUtils.getMetaDataItems(packageInfo).associateBy { it.name }
    val sourceDir = applicationInfo?.sourceDir
    val zipInfo = sourceDir?.let(::readZipInfo)

    val moduleProp = zipInfo?.moduleProp
    XposedModuleInfo(
      appName = packageInfo.getAppName(packageManager).orEmpty(),
      settingsIntent = getSettingsIntent(packageName),
      minVersion = getMinVersion(moduleProp, metadataBundle["xposedminversion"]?.source),
      targetVersion = moduleProp?.getProperty("targetApiVersion")?.takeIf(String::isNotBlank),
      staticScope = moduleProp?.getProperty("staticScope") == "true",
      defaultScope = getDefaultScope(zipInfo?.defaultScope, packageName, metadataBundle["xposedscope"]?.size),
      javaInitClasses = zipInfo?.javaInitClasses,
      nativeInitLibraries = zipInfo?.nativeInitLibraries,
      legacyInitClass = zipInfo?.legacyInitClass,
      description = getDescription(applicationInfo, moduleProp, metadataBundle["xposeddescription"]?.source)
    )
  }

  suspend fun getAlternativeLaunchItems(packageName: String): List<AlternativeLaunchItem> = withContext(Dispatchers.IO) {
    val activities = installedAppRepository.getPackageInfo(
      packageName = packageName,
      flags = PackageManager.GET_ACTIVITIES
    )?.activities ?: return@withContext emptyList()

    activities.asSequence()
      .filter(ActivityInfo::exported)
      .map {
        AlternativeLaunchItem(
          label = it.loadLabelOrName(),
          className = it.name
        )
      }
      .toList()
  }

  suspend fun getAppInfoActions(packageName: String): List<AppInfoActionItem> = withContext(Dispatchers.IO) {
    listOf(
      getShowAppInfoList(packageName),
      getShowAppSourceList(packageName),
      getShowMarketList(packageName)
    )
      .flatten()
      .distinctBy { it.packageName }
  }

  suspend fun getAppInfoPrimaryActions(packageName: String?): AppInfoPrimaryActions = withContext(Dispatchers.IO) {
    AppInfoPrimaryActions(
      launchAction = getLaunchAction(packageName),
      settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  suspend fun getAppLaunchAction(packageName: String?): AppLaunchAction? = withContext(Dispatchers.IO) {
    if (packageName.isNullOrBlank()) {
      return@withContext null
    }
    val intent = Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    val launcherActivity = PackageManagerCompat.queryIntentActivities(intent, 0)
      .firstOrNull()
      ?.activityInfo
      ?.name
      .orEmpty()
      .takeIf(String::isNotBlank)
      ?: return@withContext null
    AppLaunchAction(
      launcherActivity = launcherActivity,
      intent = intent
        .setClassName(packageName, launcherActivity)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  suspend fun getAppInstallSourceDetails(packageName: String): AppInstallSourceDetails? = withContext(Dispatchers.IO) {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return@withContext null
    AppInstallSourceDetails(
      installSource = installedAppRepository.getInstallSource(packageName),
      installedTime = packageInfo.getInstalledTimeDisplayData(
        showInstalledTime = !installedAppRepository.getPackageState(packageName).isFrozen
      ),
      dexoptInfo = packageInfo.getDexFileOptimizationInfo()
    )
  }

  suspend fun getOverlayDetail(item: LCItem): OverlayDetailResult = withContext(Dispatchers.IO) {
    val packageInfo = installedAppRepository.getPackageInfo(item.packageName)
      ?: return@withContext OverlayDetailResult.NotFound
    val applicationInfo = packageInfo.applicationInfo
      ?: return@withContext OverlayDetailResult.NotFound
    OverlayDetailResult.Available(
      OverlayDetailData(
        item = item,
        packageInfo = packageInfo,
        packageName = item.packageName,
        appName = packageInfo.getAppName(packageManager),
        versionInfo = packageInfo.getVersionString(),
        extraInfo = OverlayDetailExtraInfo(
          type = Constants.OVERLAY_STRING,
          targetSdkInfo = packageInfo.getTargetApiString(),
          minSdkInfo = applicationInfo.minSdkVersion.toString(),
          compileSdkInfo = packageInfo.getCompileSdkVersionString(),
          sizeInfo = FileUtils.getFileSize(applicationInfo.sourceDir)
            .sizeToString(context, showBytes = false)
        ),
        targetPackageName = Refine.unsafeCast<PackageInfoHidden>(packageInfo).overlayTarget
      )
    )
  }

  suspend fun getRelatedAppListItem(packageName: String): RelatedAppListItem? = withContext(Dispatchers.IO) {
    val item = appListRepository.getItem(packageName) ?: return@withContext null
    RelatedAppListItem(
      item = item,
      packageInfo = installedAppRepository.getPackageInfo(packageName)
    )
  }

  private suspend fun getLaunchAction(packageName: String?): AppInfoLaunchAction {
    if (packageName == ownPackageName) {
      return AppInfoLaunchAction.Self
    }
    return getAppLaunchAction(packageName)
      ?.let(AppInfoLaunchAction::Available)
      ?: AppInfoLaunchAction.Alternative
  }

  private fun ActivityInfo.loadLabelOrName(): String {
    return runCatching {
      loadLabel(packageManager).toString()
    }.getOrDefault(name)
  }

  private fun getShowAppInfoList(packageName: String): List<AppInfoActionItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_SHOW_APP_INFO),
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != ownPackageName }
      .map {
        toActionItem(
          packageItemInfo = it.activityInfo,
          intent = Intent(Intent.ACTION_SHOW_APP_INFO)
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowAppSourceList(packageName: String): List<AppInfoActionItem> {
    val source = installedAppRepository.getPackageInfo(packageName)
      ?.applicationInfo
      ?.sourceDir
      ?: return emptyList()
    allowFileUriExposure()
    val sourcePath = runCatching { File(source) }.getOrNull() ?: return emptyList()

    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.setDataAndType(sourcePath.toUri(), MIMETYPE_APK)
      },
      PackageManager.MATCH_DEFAULT_ONLY
    )
      .filter { isFileManager(it.activityInfo.packageName) }
      .map {
        toActionItem(
          packageItemInfo = it.activityInfo,
          intent = Intent(Intent.ACTION_VIEW)
            .setPackage(it.activityInfo.packageName)
            .setDataAndType(sourcePath.toUri(), MIMETYPE_APK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowMarketList(packageName: String): List<AppInfoActionItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.data = "market://details?id=$packageName".toUri()
      },
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != ownPackageName }
      .map {
        toActionItem(
          packageItemInfo = it.activityInfo,
          intent = Intent(Intent.ACTION_VIEW)
            .setData("market://details?id=$packageName".toUri())
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun isFileManager(packageName: String): Boolean {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType("file:///".toUri(), "*/*")
      setPackage(packageName)
    }
    val canHandleFiles = PackageManagerCompat.queryIntentActivities(
      viewIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    val permissions = installedAppRepository.getPermissions(packageName)
    val hasStoragePermission = permissions.any {
      it == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
        it == Manifest.permission.READ_EXTERNAL_STORAGE ||
        it == Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
      type = "*/*"
      setPackage(packageName)
    }
    val canPickFiles = PackageManagerCompat.queryIntentActivities(
      getContentIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    val openTreeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      setPackage(packageName)
    }
    val canManageDirectories = PackageManagerCompat.queryIntentActivities(
      openTreeIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    return (canHandleFiles || canPickFiles || canManageDirectories) && hasStoragePermission
  }

  private fun toActionItem(packageItemInfo: PackageItemInfo, intent: Intent): AppInfoActionItem {
    return AppInfoActionItem(
      packageName = packageItemInfo.packageName,
      label = packageItemInfo.loadLabel(packageManager),
      icon = getAppIcon(packageItemInfo.packageName),
      intent = intent
    )
  }

  private fun getAppIcon(packageName: String): Drawable? {
    return runCatching {
      installedAppRepository.getPackageInfo(packageName)
        ?.applicationInfo
        ?.loadIcon(packageManager)
    }.getOrNull()
  }

  private fun PackageInfo.getInstalledTimeDisplayData(
    showInstalledTime: Boolean
  ): AppInstalledTimeDisplayData? {
    if (!showInstalledTime) {
      return null
    }
    return AppInstalledTimeDisplayData(
      firstInstalledTime = firstInstallTime.toInstalledTimeText(),
      lastUpdatedTime = lastUpdateTime.toInstalledTimeText()
    )
  }

  private fun Long.toInstalledTimeText(): String {
    return if (this <= PREINSTALLED_TIMESTAMP) {
      context.getString(R.string.snapshot_preinstalled_app)
    } else {
      SimpleDateFormat.getDateTimeInstance().format(this)
    }
  }

  private fun getSettingsIntent(packageName: String): Intent? {
    val intent = Intent(Intent.ACTION_MAIN).also {
      it.`package` = packageName
      it.addCategory(CATEGORY_XPOSED_SETTINGS)
    }
    val resolveInfo = PackageManagerCompat.queryIntentActivities(intent, 0).firstOrNull()
      ?: return null

    return Intent(intent).also {
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      it.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
    }
  }

  private fun readZipInfo(sourceDir: String): XposedZipInfo {
    return runCatching {
      ZipFileCompat(sourceDir).use { zipFile ->
        XposedZipInfo(
          moduleProp = zipFile.readModuleProperties(),
          defaultScope = zipFile.readEntryLines("META-INF/xposed/scope.list"),
          javaInitClasses = zipFile.readEntryLines("META-INF/xposed/java_init.list").text,
          nativeInitLibraries = zipFile.readEntryLines("META-INF/xposed/native_init.list").text,
          legacyInitClass = zipFile.readFirstEntryLine("assets/xposed_init")
        )
      }
    }.getOrDefault(XposedZipInfo())
  }

  private fun ZipFileCompat.readModuleProperties(): Properties? {
    val entry = getEntry("META-INF/xposed/module.prop") ?: return null
    return Properties().apply {
      getInputStream(entry).use { input ->
        load(input)
      }
    }
  }

  private fun getMinVersion(moduleProp: Properties?, legacyMetadataVersion: String?): String? {
    val minVersions = listOfNotNull(
      moduleProp?.getProperty("minApiVersion")?.toIntOrNull(),
      moduleProp?.getProperty("xposedMinVersion")?.toIntOrNull(),
      legacyMetadataVersion?.toIntOrNull()
    )
    return minVersions.minOrNull()?.toString()
  }

  private fun getDefaultScope(modernScope: ZipEntryLines?, packageName: String, legacyScopeResourceId: Long?): String? {
    if (modernScope?.exists == true) {
      return modernScope.text
    }
    return legacyScopeResourceId?.let { resourceId ->
      runCatching {
        packageManager.getResourcesForApplication(packageName)
          .getStringArray(resourceId.toInt())
          .contentToString()
      }.getOrNull()
    }
  }

  private fun ZipFileCompat.readFirstEntryLine(entryName: String): String? {
    val entry = getEntry(entryName) ?: return null
    return getInputStream(entry).bufferedReader().use { reader ->
      reader.readLines().firstOrNull()?.takeIf(String::isNotBlank)
    }
  }

  private fun ZipFileCompat.readEntryLines(entryName: String): ZipEntryLines {
    val entry = getEntry(entryName) ?: return ZipEntryLines(exists = false, text = null)
    return getInputStream(entry).bufferedReader().use { reader ->
      ZipEntryLines(
        exists = true,
        text = reader.readLines()
          .filter(String::isNotBlank)
          .joinToString("\n")
          .takeIf(String::isNotBlank)
      )
    }
  }

  private fun getDescription(
    applicationInfo: ApplicationInfo?,
    moduleProp: Properties?,
    legacyDescription: String?
  ): String? {
    val resourceDescription = applicationInfo
      ?.takeIf { it.descriptionRes != 0 }
      ?.loadDescription(packageManager)
      ?.toString()

    return resourceDescription
      ?: moduleProp?.getProperty("description")
      ?: legacyDescription
        ?.takeIf { it != "null" }
        ?.takeIf(String::isNotBlank)
  }

  private companion object {
    const val CATEGORY_XPOSED_SETTINGS = "de.robv.android.xposed.category.MODULE_SETTINGS"
    const val MIMETYPE_APK = "application/vnd.android.package-archive"
  }
}

data class XposedModuleInfo(
  val appName: String,
  val settingsIntent: Intent?,
  val minVersion: String?,
  val targetVersion: String?,
  val staticScope: Boolean,
  val defaultScope: String?,
  val javaInitClasses: String?,
  val nativeInitLibraries: String?,
  val legacyInitClass: String?,
  val description: String?
)

private data class ZipEntryLines(
  val exists: Boolean,
  val text: String?
)

private data class XposedZipInfo(
  val moduleProp: Properties? = null,
  val defaultScope: ZipEntryLines = ZipEntryLines(exists = false, text = null),
  val javaInitClasses: String? = null,
  val nativeInitLibraries: String? = null,
  val legacyInitClass: String? = null
)

data class AppLaunchAction(
  val launcherActivity: String,
  val intent: Intent
)

data class AppInfoPrimaryActions(
  val launchAction: AppInfoLaunchAction,
  val settingsIntent: Intent
)

sealed interface AppInfoLaunchAction {
  data object Self : AppInfoLaunchAction
  data object Alternative : AppInfoLaunchAction
  data class Available(val action: AppLaunchAction) : AppInfoLaunchAction
}

sealed interface OverlayDetailResult {
  data class Available(val data: OverlayDetailData) : OverlayDetailResult
  data object NotFound : OverlayDetailResult
}

data class OverlayDetailData(
  val item: LCItem,
  val packageInfo: PackageInfo,
  val packageName: String,
  val appName: String?,
  val versionInfo: String,
  val extraInfo: OverlayDetailExtraInfo,
  val targetPackageName: String?
)

data class OverlayDetailExtraInfo(
  val type: String,
  val targetSdkInfo: String,
  val minSdkInfo: String,
  val compileSdkInfo: String,
  val sizeInfo: String
)

internal fun buildOverlayDetailBottomSheetDisplay(
  data: OverlayDetailData,
  targetApp: RelatedAppDisplayData?
): OverlayDetailBottomSheetDisplay {
  return OverlayDetailBottomSheetDisplay(
    item = data.item,
    applicationInfo = data.packageInfo.applicationInfo,
    packageName = data.packageName,
    appName = data.appName,
    versionInfo = data.versionInfo,
    extraInfo = OverlayDetailExtraInfoDisplay(
      type = data.extraInfo.type,
      targetSdkInfo = data.extraInfo.targetSdkInfo,
      minSdkInfo = data.extraInfo.minSdkInfo,
      compileSdkInfo = data.extraInfo.compileSdkInfo,
      sizeInfo = data.extraInfo.sizeInfo
    ),
    target = buildOverlayTargetPackageDisplay(
      item = data.item,
      targetPackageName = data.targetPackageName,
      targetApp = targetApp
    )
  )
}

internal fun buildOverlayTargetPackageDisplay(
  item: LCItem,
  targetPackageName: String?,
  targetApp: RelatedAppDisplayData?
): OverlayTargetPackageDisplay {
  return when {
    targetPackageName == null -> OverlayTargetPackageDisplay.Empty

    targetApp == null -> OverlayTargetPackageDisplay.PackageName(targetPackageName)

    else -> OverlayTargetPackageDisplay.RelatedApp(
      data = targetApp,
      showHarmonyBadge = item.variant == Constants.VARIANT_HAP
    )
  }
}
