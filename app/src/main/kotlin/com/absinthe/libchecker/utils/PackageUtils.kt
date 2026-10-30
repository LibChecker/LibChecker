package com.absinthe.libchecker.utils

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.StatefulComponent
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.manifest.StaticLibraryReader
import java.io.File
import timber.log.Timber

object PackageUtils {

  /**
   * Get packageInfo
   * @param info ApplicationInfo
   * @param flag Flag mask
   * @return PackageInfo
   * @throws PackageManager.NameNotFoundException
   */
  @Throws(PackageManager.NameNotFoundException::class)
  fun getPackageInfo(info: ApplicationInfo, flag: Int = 0): PackageInfo {
    return getPackageInfo(info.packageName, flag)
  }

  /**
   * Get packageInfo
   * @param packageName Package name string
   * @param flag Flag mask
   * @param needAchieve Whether need to achieve frozen app info
   * @return PackageInfo
   * @throws PackageManager.NameNotFoundException
   */
  @Throws(PackageManager.NameNotFoundException::class)
  fun getPackageInfo(packageName: String, flag: Int = 0, needAchieve: Boolean = true): PackageInfo {
    val defaultFlags = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES
    val packageInfo = PackageManagerCompat.getPackageInfo(
      packageName,
      defaultFlags or flag
    ).also {
      it.applicationInfo?.let { ai ->
        if (needAchieve && FreezeUtils.isAppFrozen(ai)) {
          return PackageManagerCompat.getPackageArchiveInfo(
            ai.sourceDir,
            PackageManager.MATCH_DISABLED_COMPONENTS or flag
          )?.apply {
            applicationInfo?.let { appInfo ->
              val rootDir = File(ai.sourceDir).parentFile!!
              appInfo.enabled = false
              appInfo.sourceDir = ai.sourceDir
              appInfo.nativeLibraryDir = ai.nativeLibraryDir
              appInfo.splitSourceDirs = rootDir.listFiles().orEmpty()
                .filter { file -> file.name.startsWith("split_") && file.extension == "apk" }
                .map { file -> file.path }
                .toTypedArray()
            }
          } ?: throw PackageManager.NameNotFoundException()
        }
      }
    }

    return packageInfo
  }

  /**
   * Get version code of an app
   * @param packageName packageName
   * @return version code as Long Integer
   */
  fun getVersionCode(packageName: String): Long {
    return getPackageInfo(packageName).getVersionCode()
  }

  /**
   * Get split apks dirs
   * @param packageInfo PackageInfo
   * @return List of split apks dirs
   */
  fun getSplitsSourceDir(packageInfo: PackageInfo): Array<String>? {
    val ai = packageInfo.applicationInfo ?: return null
    if (FreezeUtils.isAppFrozen(ai)) {
      File(ai.sourceDir).parentFile?.takeIf { it.exists() }?.let { files ->
        return files.listFiles { file -> file.name.startsWith("split_") && file.name.endsWith(".apk") }
          ?.let { list -> Array(list.size) { list[it].absolutePath } }
      }
    }
    return ai.splitSourceDirs
  }

  /**
   * Get static libraries which app uses
   * @param packageInfo PackageInfo
   * @return static libraries list
   */
  fun getStaticLibs(packageInfo: PackageInfo): List<LibStringItem> {
    val sharedLibs = packageInfo.applicationInfo?.sharedLibraryFiles ?: return emptyList()
    try {
      val demands =
        StaticLibraryReader.getStaticLibrary(File(packageInfo.applicationInfo!!.sourceDir))
      if (demands.isNullOrEmpty() || sharedLibs.isEmpty()) {
        return listOf()
      }

      return demands.map {
        it.value.path = sharedLibs.find { shared -> shared.contains(it.key) }.orEmpty()
        LibStringItem(name = it.key, size = 0L, source = it.value.toJson())
      }
    } catch (e: Exception) {
      Timber.e(e)
      return emptyList()
    }
  }

  /**
   * Get all meta data in an app
   * @param packageInfo PackageInfo
   * @return meta data list
   */
  fun getMetaDataItems(packageInfo: PackageInfo): List<LibStringItem> {
    val ai = packageInfo.applicationInfo ?: return emptyList()
    val metadata = ai.metaData ?: return emptyList()
    val appResources by lazy { SystemServices.packageManager.getResourcesForApplication(ai) }

    return metadata.keySet().asSequence()
      .map { key ->
        @Suppress("DEPRECATION")
        var value = metadata.get(key).toString()
        var id = 0L

        if (value.maybeResourceId()) {
          id = value.toLong()
          runCatching {
            value = appResources.getResourceName(id.toInt())
          }
        }
        LibStringItem(key, id, value)
      }
      .toList()
  }

  /**
   * Get components list of an app
   * @param packageName Package name of the app
   * @param type Component type
   * @param isSimpleName Whether to show class name as a simple name
   * @return List of StatefulComponent
   */
  fun getComponentList(
    packageName: String,
    @LibType type: Int,
    isSimpleName: Boolean
  ): List<StatefulComponent> {
    return runCatching {
      getComponentList(getPackageInfo(packageName, getComponentPackageInfoFlag(type)), type, isSimpleName)
    }.getOrElse { emptyList() }
  }

  /**
   * Get components list of an app
   * @param packageName Package name of the app
   * @param type Component type
   * @param isSimpleName Whether to show class name as a simple name
   * @return List of String
   */
  fun getComponentStringList(
    packageName: String,
    @LibType type: Int,
    isSimpleName: Boolean
  ): List<String> {
    return runCatching {
      getComponentStringList(getPackageInfo(packageName, getComponentPackageInfoFlag(type)), type, isSimpleName)
    }.getOrElse { emptyList() }
  }

  internal fun getComponentPackageInfoFlag(@LibType type: Int): Int {
    return when (type) {
      SERVICE -> PackageManager.GET_SERVICES
      ACTIVITY -> PackageManager.GET_ACTIVITIES
      RECEIVER -> PackageManager.GET_RECEIVERS
      PROVIDER -> PackageManager.GET_PROVIDERS
      else -> 0
    }
  }

  /**
   * Get components list of an app
   * @param packageInfo PackageInfo
   * @param type Component type
   * @param isSimpleName Whether to show class name as a simple name
   * @return List of StatefulComponent
   */
  private fun getComponentList(
    packageInfo: PackageInfo,
    @LibType type: Int,
    isSimpleName: Boolean
  ): List<StatefulComponent> {
    val list: Array<out ComponentInfo>? = when (type) {
      SERVICE -> packageInfo.services
      ACTIVITY -> packageInfo.activities
      RECEIVER -> packageInfo.receivers
      PROVIDER -> packageInfo.providers
      else -> null
    }

    return getComponentList(packageInfo.packageName, list, isSimpleName)
  }

  /**
   * Get components list of an app
   * @param packageInfo PackageInfo
   * @param type Component type
   * @param isSimpleName Whether to show class name as a simple name
   * @return List of String
   */
  fun getComponentStringList(
    packageInfo: PackageInfo,
    @LibType type: Int,
    isSimpleName: Boolean
  ): List<String> {
    val list: Array<out ComponentInfo>? = when (type) {
      SERVICE -> packageInfo.services
      ACTIVITY -> packageInfo.activities
      RECEIVER -> packageInfo.receivers
      PROVIDER -> packageInfo.providers
      else -> null
    }

    return getComponentStringList(packageInfo.packageName, list, isSimpleName)
  }

  /**
   * Check if a component is enabled
   * @param info ComponentInfo
   * @return true if it is enabled
   */
  fun isComponentEnabled(info: ComponentInfo): Boolean {
    val state = runCatching {
      SystemServices.packageManager.getComponentEnabledSetting(
        ComponentName(info.packageName, info.name)
      )
    }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    return when (state) {
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
      PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> info.enabled
      else -> false
    }
  }

  /**
   * Check if a component is exported
   * @param info ComponentInfo
   * @return true if it is exported
   */
  fun isComponentExported(info: ComponentInfo): Boolean {
    return runCatching {
      info.exported
    }.getOrElse {
      Timber.e(it)
      false
    }
  }

  /**
   * Get components list of an app
   * @param packageName Package name of the app
   * @param list List of components(can be nullable)
   * @param isSimpleName Whether to show class name as a simple name
   * @return List of StatefulComponent
   */
  fun getComponentList(
    packageName: String,
    list: Array<out ComponentInfo>?,
    isSimpleName: Boolean
  ): List<StatefulComponent> {
    if (list.isNullOrEmpty()) {
      return emptyList()
    }
    return list.map {
      val name = if (isSimpleName) {
        it.name.orEmpty().shortenComponentName(packageName)
      } else {
        it.name.orEmpty()
      }
      StatefulComponent(
        name,
        isComponentEnabled(it),
        isComponentExported(it),
        it.processName.orEmpty().removePrefix(it.packageName)
      )
    }
  }

  fun getComponentList(
    packageName: String,
    list: List<String>,
    isSimpleName: Boolean
  ): List<StatefulComponent> {
    if (list.isEmpty()) {
      return emptyList()
    }
    return list.map {
      val name = if (isSimpleName) {
        it.shortenComponentName(packageName)
      } else {
        it
      }
      StatefulComponent(componentName = name, enabled = true, exported = true, processName = "")
    }
  }

  /**
   * Get components list of an app
   * @param packageName Package name of the app
   * @param list List of components(can be nullable)
   * @param isSimpleName Whether to show class name as a simple name
   * @return List of String
   */
  private fun getComponentStringList(
    packageName: String,
    list: Array<out ComponentInfo>?,
    isSimpleName: Boolean
  ): List<String> {
    if (list.isNullOrEmpty()) {
      return emptyList()
    }
    return list.map {
      if (isSimpleName) {
        it.name.shortenComponentName(packageName)
      } else {
        it.name
      }
    }
  }

  private fun String.shortenComponentName(packageName: String): String {
    return if (packageName.isNotEmpty() &&
      length > packageName.length &&
      this[packageName.length] == '.' &&
      startsWith(packageName)
    ) {
      substring(packageName.length)
    } else {
      this
    }
  }

  /**
   * Get permissions of an application
   * @param packageName Package name of the app
   * @return Permissions list
   */
  fun getPermissionsList(packageName: String): List<String> {
    return runCatching {
      getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).getPermissionsList()
    }.getOrElse { emptyList() }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  fun getInstallSourceInfo(packageName: String): InstallSourceInfo? {
    val origInstallSourceInfo = runCatching {
      SystemServices.packageManager.getInstallSourceInfo(packageName)
    }.getOrElse { e ->
      Timber.e(e)
      return null
    }
    if (!ShizukuManager.requireAvailable()) {
      return origInstallSourceInfo
    }
    return ShizukuManager.getPackageManager().let {
      if (OsUtils.atLeastU()) {
        it.getInstallSourceInfo(packageName, ShizukuManager.getUid())
      } else {
        it.getInstallSourceInfo(packageName)
      }
    }
  }

  /**
   * Check if an app is installed
   * @return true if it is installed
   */
  fun isAppInstalled(pkgName: String): Boolean {
    return runCatching {
      PackageManagerCompat.getApplicationInfo(pkgName, 0).enabled
    }.getOrDefault(false)
  }
}
