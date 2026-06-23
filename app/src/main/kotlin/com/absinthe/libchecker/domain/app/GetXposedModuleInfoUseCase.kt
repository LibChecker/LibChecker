package com.absinthe.libchecker.domain.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import java.util.Properties

class GetXposedModuleInfoUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageName: String): XposedModuleInfo? {
    val packageInfo = installedAppRepository.getPackageInfo(packageName, PackageManager.GET_META_DATA)
      ?: return null
    val applicationInfo = packageInfo.applicationInfo
    val metadataBundle = PackageUtils.getMetaDataItems(packageInfo).associateBy { it.name }
    val sourceDir = applicationInfo?.sourceDir
    val zipInfo = sourceDir?.let(::readZipInfo)

    val moduleProp = zipInfo?.moduleProp
    return XposedModuleInfo(
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
