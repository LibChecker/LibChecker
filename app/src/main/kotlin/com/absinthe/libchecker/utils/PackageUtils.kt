package com.absinthe.libchecker.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfoHidden
import android.content.pm.ComponentInfo
import android.content.pm.IPackageManager
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Process
import android.text.format.Formatter
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.collection.arrayMapOf
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ET_CORE
import com.absinthe.libchecker.annotation.ET_DYN
import com.absinthe.libchecker.annotation.ET_EXEC
import com.absinthe.libchecker.annotation.ET_HIPROC
import com.absinthe.libchecker.annotation.ET_LOPROC
import com.absinthe.libchecker.annotation.ET_NONE
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ET_REL
import com.absinthe.libchecker.annotation.ElfType
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV5_STRING
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV7_STRING
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.ARMV8_STRING
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.constant.Constants.X86_64_STRING
import com.absinthe.libchecker.constant.Constants.X86_STRING
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.dex.DexLibMap
import com.absinthe.libchecker.utils.dex.FastDexFileFactory
import com.absinthe.libchecker.utils.elf.ELFParser
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isOverlay
import com.absinthe.libchecker.utils.extensions.isUse32BitAbi
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.libchecker.utils.extensions.sha1
import com.absinthe.libchecker.utils.extensions.sha256
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.extensions.toHexString
import com.absinthe.libchecker.utils.manifest.StaticLibraryReader
import com.android.tools.smali.dexlib2.Opcodes
import dev.rikka.tools.refine.Refine
import java.io.File
import java.io.InputStream
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.DateFormat
import java.util.zip.ZipEntry
import javax.security.cert.X509Certificate
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
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
   * @return PackageInfo
   * @throws PackageManager.NameNotFoundException
   */
  @Throws(PackageManager.NameNotFoundException::class)
  fun getPackageInfo(packageName: String, flag: Int = 0): PackageInfo {
    val packageInfo = PackageManagerCompat.getPackageInfo(
      packageName,
      PackageManager.MATCH_DISABLED_COMPONENTS or flag
    )
    if (FreezeUtils.isAppFrozen(packageInfo.applicationInfo)) {
      return PackageManagerCompat.getPackageArchiveInfo(
        packageInfo.applicationInfo.sourceDir,
        PackageManager.MATCH_DISABLED_COMPONENTS or flag
      )?.apply {
        applicationInfo.sourceDir = packageInfo.applicationInfo.sourceDir
        applicationInfo.nativeLibraryDir = packageInfo.applicationInfo.nativeLibraryDir
      } ?: throw PackageManager.NameNotFoundException()
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
   * Get version string of an app ( 1.0.0(1) )
   * @param versionName Version name
   * @param versionCode Version code
   * @return version code as String
   */
  fun getVersionString(versionName: String, versionCode: Long): String {
    return "$versionName ($versionCode)"
  }

  /**
   * Get native libraries of an app
   * @param packageInfo PackageInfo
   * @param needStaticLibrary True if need get static libraries
   * @param specifiedAbi Specify an ABI
   * @return List of LibStringItem
   */
  fun getNativeDirLibs(
    packageInfo: PackageInfo,
    needStaticLibrary: Boolean = false,
    specifiedAbi: Int? = null
  ): List<LibStringItem> {
    val nativePath = packageInfo.applicationInfo.nativeLibraryDir
    val list = mutableListOf<LibStringItem>()

    if (nativePath != null) {
      File(nativePath).listFiles()?.let { files ->
        list.addAll(
          files.asSequence()
            .filter { it.isFile }
            .distinctBy { it.name }
            .map {
              val elfParser = runCatching { ELFParser(it.inputStream()) }.getOrNull()
              LibStringItem(
                name = it.name,
                size = FileUtils.getFileSize(it),
                elfType = elfParser?.getEType() ?: ET_NOT_ELF,
                elfClass = elfParser?.getEClass() ?: ELFParser.EIdent.ELFCLASSNONE
              )
            }
            .toMutableList()
        )
      }
    }

    if (list.isEmpty()) {
      var abi = specifiedAbi ?: runCatching { getAbi(packageInfo) }.getOrNull() ?: return emptyList()

      if (abi == NO_LIBS) {
        abi = if (Process.is64Bit()) ARMV8 else ARMV7
      }
      val abiString = getAbiString(LibCheckerApp.app, abi, false)
      list.addAll(
        getSourceLibs(
          packageInfo = packageInfo,
          childDir = "lib/$abiString"
        )
      )
    }
    list.addAll(
      getSourceLibs(
        packageInfo = packageInfo,
        childDir = "assets/",
        source = "/assets"
      )
    )

    if (needStaticLibrary) {
      list.addAll(getStaticLibs(packageInfo))
    }

    return list.distinctBy { it.name }
  }

  /**
   * Get native libraries of an app from source path
   * @param packageInfo PackageInfo
   * @return List of LibStringItem
   */
  private fun getSourceLibs(
    packageInfo: PackageInfo,
    childDir: String,
    source: String? = null
  ): List<LibStringItem> {
    if (packageInfo.applicationInfo.sourceDir == null) {
      return emptyList()
    }
    val file = File(packageInfo.applicationInfo.sourceDir)
    if (file.exists().not()) {
      return emptyList()
    }
    return runCatching {
      ZipFileCompat(file).use { zipFile ->
        return zipFile.getZipEntries()
          .asSequence()
          .filter { (it.isDirectory.not() && it.name.startsWith(childDir)) && it.name.endsWith(".so") }
          .distinctBy { it.name.split("/").last() }
          .map {
            val elfParser = runCatching { getElfParser(zipFile.getInputStream(it)) }.getOrNull()
            LibStringItem(
              name = it.name.split("/").last(),
              size = it.size,
              source = source,
              elfType = elfParser?.getEType() ?: ET_NOT_ELF,
              elfClass = elfParser?.getEClass() ?: ELFParser.EIdent.ELFCLASSNONE
            )
          }
          .toList()
          .ifEmpty { getSplitLibs(packageInfo) }
      }
    }.onFailure {
      Timber.e(it)
    }.getOrElse { emptyList() }
  }

  /**
   * Get native libraries of an app from split apk
   * @param packageInfo PackageInfo
   * @return List of LibStringItem
   */
  private fun getSplitLibs(packageInfo: PackageInfo): List<LibStringItem> {
    val libList = mutableListOf<LibStringItem>()
    val splitList = getSplitsSourceDir(packageInfo)
    if (splitList.isNullOrEmpty()) {
      return listOf()
    }

    splitList.filter {
      val fileName = it.split(File.separator).last()
      fileName.contains("arm") || fileName.contains("x86")
    }.forEach {
      ZipFileCompat(File(it)).use { zipFile ->
        zipFile.getZipEntries().asSequence().forEach { entry ->
          if (entry.name.startsWith("lib/") && entry.isDirectory.not()) {
            val elfParser = runCatching { getElfParser(zipFile.getInputStream(entry)) }.getOrNull()
            val fileName = it.split(File.separator).last()
            libList.add(
              LibStringItem(
                name = entry.name.split("/").last(),
                size = entry.size,
                process = if (fileName.startsWith("split_config")) null else fileName,
                elfType = elfParser?.getEType() ?: ET_NOT_ELF,
                elfClass = elfParser?.getEClass() ?: ELFParser.EIdent.ELFCLASSNONE
              )
            )
          }
        }
      }
    }

    return libList
  }

  private val regex_splits by lazy { Regex("split_config\\.(.*)\\.apk") }

  /**
   * Get split apks dirs
   * @param packageInfo PackageInfo
   * @return List of split apks dirs
   */
  fun getSplitsSourceDir(packageInfo: PackageInfo): Array<String>? {
    if (FreezeUtils.isAppFrozen(packageInfo.applicationInfo)) {
      File(packageInfo.applicationInfo.sourceDir).parentFile?.takeIf { it.exists() }?.let { files ->
        return files.listFiles { file -> file.name.matches(regex_splits) }
          ?.map { it.absolutePath }
          ?.toTypedArray()
      }
    }
    return packageInfo.applicationInfo.splitSourceDirs
  }

  const val STATIC_LIBRARY_SOURCE_PREFIX = "[Path] "
  const val VERSION_CODE_PREFIX = "[Version Code] "

  /**
   * Get static libraries which app uses
   * @param packageInfo PackageInfo
   * @return static libraries list
   */
  fun getStaticLibs(packageInfo: PackageInfo): List<LibStringItem> {
    val sharedLibs = packageInfo.applicationInfo.sharedLibraryFiles
    try {
      val demands =
        StaticLibraryReader.getStaticLibrary(File(packageInfo.applicationInfo.sourceDir))
      if (demands.isNullOrEmpty() || sharedLibs.isNullOrEmpty()) {
        return listOf()
      }

      val list = mutableListOf<LibStringItem>()
      demands.forEach {
        val source = sharedLibs.find { shared -> shared.contains(it.key) }
        if (source != null) {
          list.add(
            LibStringItem(
              name = it.key,
              size = 0L,
              source = "$STATIC_LIBRARY_SOURCE_PREFIX$source\n$VERSION_CODE_PREFIX${it.value}"
            )
          )
        }
      }
      return list
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
    val appResources by lazy { SystemServices.packageManager.getResourcesForApplication(packageInfo.applicationInfo) }
    packageInfo.applicationInfo.metaData?.let {
      return it.keySet().asSequence()
        .map { key ->
          @Suppress("DEPRECATION")
          var value = it.get(key).toString()
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
    } ?: return emptyList()
  }

  /**
   * Check if an app uses Kotlin language from classes.dex
   * @param file APK file of the app
   * @return true if it uses Kotlin language
   */
  fun isKotlinUsedInClassDex(file: File): Boolean {
    return findDexClasses(
      file,
      listOf("kotlin.*".toClassDefType(), "kotlinx.*".toClassDefType())
    ).isNotEmpty()
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
    val flag = when (type) {
      SERVICE -> PackageManager.GET_SERVICES
      ACTIVITY -> PackageManager.GET_ACTIVITIES
      RECEIVER -> PackageManager.GET_RECEIVERS
      PROVIDER -> PackageManager.GET_PROVIDERS
      else -> 0
    }

    return runCatching {
      getComponentList(getPackageInfo(packageName, flag), type, isSimpleName)
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
    val flag = when (type) {
      SERVICE -> PackageManager.GET_SERVICES
      ACTIVITY -> PackageManager.GET_ACTIVITIES
      RECEIVER -> PackageManager.GET_RECEIVERS
      PROVIDER -> PackageManager.GET_PROVIDERS
      else -> 0
    }

    return runCatching {
      getComponentStringList(getPackageInfo(packageName, flag), type, isSimpleName)
    }.getOrElse { emptyList() }
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
    return list.asSequence()
      .map {
        val name = if (isSimpleName) {
          it.name.orEmpty().removePrefix(packageName)
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
      .toList()
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
    return list.asSequence()
      .map {
        if (isSimpleName) {
          it.name.removePrefix(packageName)
        } else {
          it.name
        }
      }
      .toList()
  }

  /**
   * Get ABIs set of an app
   * @param file Application file
   * @param packageInfo PackageInfo
   * @param isApk Whether is an APK file
   * @param ignoreArch Ignore arch so you can get all ABIs
   * @return ABI type
   */
  fun getAbiSet(
    file: File,
    packageInfo: PackageInfo,
    isApk: Boolean = false,
    ignoreArch: Boolean = false
  ): Set<Int> {
    var elementName: String

    val abiSet = mutableSetOf<Int>()
    var zipFile: IZipFile? = null

    if (file.exists().not()) {
      Timber.w("File not exists: ${file.absolutePath}")
      return abiSet
    }

    try {
      zipFile = ZipFileCompat(file)
      val entries = zipFile.getZipEntries()

      if (packageInfo.isOverlay()) {
        abiSet.add(OVERLAY)
        return abiSet
      }

      var entry: ZipEntry

      while (entries.hasMoreElements()) {
        entry = entries.nextElement()

        if (entry.isDirectory) {
          continue
        }

        elementName = entry.name

        if (abiSet.size == 5) {
          break
        }

        if (elementName.startsWith("lib/")) {
          elementName = elementName.removePrefix("lib/")
          when {
            elementName.startsWith("$ARMV8_STRING/") -> {
              if (Build.SUPPORTED_ABIS.contains(ARMV8_STRING) || ignoreArch) {
                abiSet.add(ARMV8)
              }
            }

            elementName.startsWith("$ARMV7_STRING/") -> {
              if (Build.SUPPORTED_ABIS.contains(ARMV7_STRING) || ignoreArch) {
                abiSet.add(ARMV7)
              }
            }

            elementName.startsWith("$ARMV5_STRING/") -> {
              if (Build.SUPPORTED_ABIS.contains(ARMV5_STRING) || ignoreArch) {
                abiSet.add(ARMV5)
              }
            }

            elementName.startsWith("$X86_64_STRING/") -> {
              if (Build.SUPPORTED_ABIS.contains(X86_64_STRING) || ignoreArch) {
                abiSet.add(X86_64)
              }
            }

            elementName.startsWith("$X86_STRING/") -> {
              if (Build.SUPPORTED_ABIS.contains(X86_STRING) || ignoreArch) {
                abiSet.add(X86)
              }
            }
          }
        }
      }

      if (abiSet.isEmpty()) {
        if (!isApk && packageInfo.applicationInfo.nativeLibraryDir != null) {
          abiSet.addAll(getAbiListByNativeDir(packageInfo.applicationInfo.nativeLibraryDir))
        }

        if (abiSet.isEmpty()) {
          abiSet.add(NO_LIBS)
        }
      }
      return abiSet
    } catch (e: Throwable) {
      Timber.e(e)
      abiSet.clear()
      abiSet.add(ERROR)
      return abiSet
    } finally {
      zipFile?.close()
    }
  }

  /**
   * Get ABI type of an app from native path
   * @param nativePath Native path of the app
   * @return ABI type
   */
  private fun getAbiListByNativeDir(nativePath: String): MutableSet<Int> {
    val file = File(nativePath.substring(0, nativePath.lastIndexOf("/")))
    val abis = mutableSetOf<Int>()

    val fileList = file.listFiles() ?: return mutableSetOf()

    fileList.asSequence()
      .forEach {
        if (it.isDirectory) {
          when (it.name) {
            "arm64" -> abis.add(ARMV8)
            "arm" -> abis.add(ARMV7)
            "x86_64" -> abis.add(X86_64)
            "x86" -> abis.add(X86)
          }
        }
      }

    return abis
  }

  /**
   * Get ABI type of an app
   * @param packageInfo PackageInfo
   * @return ABI type
   */
  fun getAbi(
    packageInfo: PackageInfo,
    isApk: Boolean = false,
    abiSet: Set<Int>? = null
  ): Int {
    val applicationInfo: ApplicationInfo = packageInfo.applicationInfo
    val overlay = packageInfo.isOverlay()

    if (overlay) {
      return OVERLAY
    }

    if (applicationInfo.sourceDir == null) {
      throw IllegalStateException("sourceDir is null: ${packageInfo.packageName}")
    }

    val file = File(applicationInfo.sourceDir)
    val realAbiSet = abiSet ?: getAbiSet(file, packageInfo, isApk, ignoreArch = true)

    if (file.exists().not() || realAbiSet.contains(NO_LIBS)) {
      return NO_LIBS
    }

    val use32bitAbi = applicationInfo.isUse32BitAbi()
    val multiArch = applicationInfo.flags and ApplicationInfo.FLAG_MULTIARCH != 0

    var abi = when (Refine.unsafeCast<ApplicationInfoHidden>(applicationInfo).primaryCpuAbi) {
      ARMV8_STRING -> ARMV8

      ARMV7_STRING -> ARMV7

      ARMV5_STRING -> ARMV5

      X86_64_STRING -> X86_64

      X86_STRING -> X86

      null -> {
        val supportedAbiSet = mutableSetOf<Int>()
        realAbiSet.forEach {
          if (Build.SUPPORTED_ABIS.contains(getAbiString(LibCheckerApp.app, it, false))) {
            supportedAbiSet.add(it)
          }
        }
        if (use32bitAbi) {
          supportedAbiSet.remove(ARMV8)
          supportedAbiSet.remove(X86_64)
        }
        when {
          supportedAbiSet.contains(ARMV8) -> ARMV8
          supportedAbiSet.contains(ARMV7) -> ARMV7
          supportedAbiSet.contains(ARMV5) -> ARMV5
          supportedAbiSet.contains(X86_64) -> X86_64
          supportedAbiSet.contains(X86) -> X86
          else -> ERROR
        }
      }

      else -> ERROR
    }

    if (multiArch) {
      abi += MULTI_ARCH
    }
    return abi
  }

  private val ABI_STRING_RES_MAP = arrayMapOf(
    ERROR to listOf(R.string.cannot_read),
    NO_LIBS to listOf(R.string.no_libs),
    ARMV8 to listOf(R.string.arm64_v8a),
    X86_64 to listOf(R.string.x86_64),
    ARMV7 to listOf(R.string.armeabi_v7a),
    ARMV5 to listOf(R.string.armeabi),
    X86 to listOf(R.string.x86),
    ARMV8 + MULTI_ARCH to listOf(R.string.arm64_v8a, R.string.multiArch),
    ARMV7 + MULTI_ARCH to listOf(R.string.armeabi_v7a, R.string.multiArch),
    ARMV5 + MULTI_ARCH to listOf(R.string.armeabi, R.string.multiArch),
    X86_64 + MULTI_ARCH to listOf(R.string.x86_64, R.string.multiArch),
    X86 + MULTI_ARCH to listOf(R.string.x86, R.string.multiArch)
  )

  private val ABI_BADGE_MAP = arrayMapOf(
    ERROR to 0,
    NO_LIBS to if (Process.is64Bit()) R.drawable.ic_abi_label_64bit else R.drawable.ic_abi_label_32bit,
    ARMV8 to R.drawable.ic_abi_label_64bit,
    X86_64 to R.drawable.ic_abi_label_64bit,
    ARMV7 to R.drawable.ic_abi_label_32bit,
    ARMV5 to R.drawable.ic_abi_label_32bit,
    X86 to R.drawable.ic_abi_label_32bit,
    OVERLAY to R.drawable.ic_abi_label_no_libs,
    ARMV8 + MULTI_ARCH to R.drawable.ic_abi_label_64bit,
    X86_64 + MULTI_ARCH to R.drawable.ic_abi_label_64bit,
    ARMV7 + MULTI_ARCH to R.drawable.ic_abi_label_32bit,
    ARMV5 + MULTI_ARCH to R.drawable.ic_abi_label_32bit,
    X86 + MULTI_ARCH to R.drawable.ic_abi_label_32bit
  )

  /**
   * Get ABI string from ABI type
   * @param context Context
   * @param abi ABI type
   * @param showExtraInfo show "multiArch" etc. if is true
   * @return ABI string
   */
  fun getAbiString(context: Context, abi: Int, showExtraInfo: Boolean): String {
    if (abi == OVERLAY) {
      return Constants.OVERLAY_STRING
    }
    val resList = if (!showExtraInfo && abi >= MULTI_ARCH) {
      ABI_STRING_RES_MAP[abi % MULTI_ARCH] ?: listOf(R.string.unknown)
    } else {
      ABI_STRING_RES_MAP[abi] ?: listOf(R.string.unknown)
    }
    return resList.joinToString { context.getString(it) }
  }

  /**
   * Get ABI badge resource from ABI type
   * @param type ABI type
   * @return Badge resource
   */
  @DrawableRes
  fun getAbiBadgeResource(type: Int): Int {
    return ABI_BADGE_MAP[type] ?: 0
  }

  /**
   * Format size number to string
   * @param item LibStringItem
   * @return String of size number (100KB)
   */
  fun sizeToString(
    context: Context,
    item: LibStringItem,
    showElfInfo: Boolean = true,
    is64Bit: Boolean = false
  ): String {
    val source = item.source?.let { "[${item.source}]" }.orEmpty()
    if (showElfInfo.not()) {
      return "(${Formatter.formatFileSize(context, item.size)}) $source"
    }
    val elfType =
      "[${elfTypeToString(item.elfType)}]"
        .takeIf { item.elfType != ET_DYN }
        .orEmpty()
    val elfClass =
      "[${elfClassToString(item.elfClass)}]"
        .takeIf {
          item.elfType != ET_NOT_ELF &&
            ((is64Bit && item.elfClass == ELFParser.EIdent.ELFCLASS32) || (!is64Bit && item.elfClass == ELFParser.EIdent.ELFCLASS64))
        }
        .orEmpty()

    return "(${Formatter.formatFileSize(context, item.size)}) $source $elfType $elfClass"
  }

  /**
   * Format ELF type to string
   * @param type ELF type
   * @return String of ELF type
   */
  private fun elfTypeToString(@ElfType type: Int): String {
    return when (type) {
      ET_NOT_ELF -> "Not ELF"
      ET_NONE -> "No file type"
      ET_REL -> "Relocatable file"
      ET_EXEC -> "Executable file"
      ET_DYN -> "Shared object file"
      ET_CORE -> "Core file"
      ET_LOPROC -> "Processor-specific"
      ET_HIPROC -> "Processor-specific"
      else -> "Not Standard ELF"
    }
  }

  /**
   * Format ELF class to string
   * @param elfClass ELF class
   * @return String of ELF class
   */
  private fun elfClassToString(elfClass: Int): String {
    return when (elfClass) {
      ELFParser.EIdent.ELFCLASSNONE -> "ELFCLASSNONE"
      ELFParser.EIdent.ELFCLASS32 -> "ELFCLASS32"
      ELFParser.EIdent.ELFCLASS64 -> "ELFCLASS64"
      else -> "Not Standard ELF"
    }
  }

  /**
   * Check if a package contains a class
   * @param sourceFile Source file
   * @param classes Class name
   * @param hasAny true if has any class, false if has all classes
   */
  fun findDexClasses(
    sourceFile: File,
    classes: List<String>,
    hasAny: Boolean = false
  ): List<String> {
    val findList = mutableListOf<String>()
    return runCatching {
      FastDexFileFactory.loadDexContainer(sourceFile, Opcodes.getDefault()).apply {
        dexEntryNames.forEach { entry ->
          getEntry(entry)?.dexFile?.classes?.forEach { def ->
            classes.forEach {
              val foundClass = if (it.last() == '*') {
                def.type.startsWith(it.removeSuffix("*"))
              } else {
                def.type == it
              }
              if (foundClass && !findList.contains(it)) {
                findList.add(it)

                if (findList.size == classes.size || hasAny) {
                  return@runCatching findList
                }
              }
            }
          }
        }
      }
      return findList
    }.getOrDefault(emptyList())
  }

  /**
   * Get part of DEX classes (at most 5 DEX's) of an app
   * @param packageName Package name
   * @param isApk True if it is an apk file
   * @return List of LibStringItem
   */
  fun getDexList(packageName: String, isApk: Boolean = false): List<LibStringItem> {
    try {
      val path = if (isApk) {
        packageName
      } else {
        getPackageInfo(packageName).applicationInfo.sourceDir
      }

      if (path.isNullOrEmpty()) {
        return emptyList()
      }
      var className: String

      val primaryList = mutableListOf<LibStringItem>()
      val pkgType = "L${packageName.replace(".", "/")}"
      FastDexFileFactory.loadDexContainer(File(path), Opcodes.getDefault()).apply {
        dexEntryNames.forEachIndexed { index, entry ->
          if (index >= 5) {
            return@forEachIndexed
          }
          getEntry(entry)?.let { dexEntry ->
            primaryList += dexEntry.dexFile.classes
              .asSequence()
              .filter { !it.type.startsWith(pkgType) }
              .map { item ->
                className = item.type.substring(1, item.length - 1).replace("/", ".")
                when {
                  // Remove obfuscated classes
                  !className.contains(".") -> LibStringItem("")

                  // Remove kotlin
                  className.startsWith("kotlin") -> LibStringItem("")

                  // Merge AndroidX classes
                  className.startsWith("androidx") -> LibStringItem(
                    className.substring(
                      0,
                      className.indexOf(".", 9).takeIf { it != -1 } ?: className.length
                    )
                  )

                  // Filter classes which paths deep level greater than 4
                  else -> LibStringItem(
                    className.split(".").run {
                      this.subList(0, this.size.coerceAtMost(4)).joinToString(separator = ".")
                    }
                  )
                }
              }
              .toSet()
              .filter { it.name.isNotBlank() }
              .toMutableList()
          }
        }
      }

      // Merge path deep level 3 classes
      primaryList.filter { it.name.split(".").size == 3 }.forEach {
        primaryList.removeAll { item -> item.name.startsWith(it.name) }
        primaryList.add(it)
      }
      // Merge path deep level 4 classes
      var pathLevel3Item: String
      var filter: List<LibStringItem>
      primaryList.filter { it.name.split(".").size == 4 }.forEach {
        if (DexLibMap.DEEP_LEVEL_3_SET.contains(it.name)) {
          return@forEach
        }

        pathLevel3Item = it.name.split(".").subList(0, 3).joinToString(separator = ".")
        filter = primaryList.filter { item -> item.name.startsWith(pathLevel3Item) }

        if (filter.isNotEmpty()) {
          primaryList.removeAll(filter.toSet())
          primaryList.add(LibStringItem(pathLevel3Item))
        }
      }
      return primaryList
    } catch (e: Exception) {
      Timber.e(e)
      return emptyList()
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

  /**
   * Get permissions of an application with granted state
   * @param packageName Package name of the app
   * @return Permissions list with granted state
   */
  fun getStatefulPermissionsList(packageName: String): List<Pair<String, Boolean>> {
    return runCatching {
      getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).getStatefulPermissionsList()
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
    if (!Shizuku.pingBinder()) {
      Timber.e("Shizuku not running")
      return origInstallSourceInfo
    }
    if (Shizuku.getVersion() < 10) {
      Timber.e("Requires Shizuku API 10")
      return origInstallSourceInfo
    } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
      Timber.i("Shizuku not authorized")
      return origInstallSourceInfo
    }
    return IPackageManager.Stub.asInterface(
      ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
    ).let {
      if (OsUtils.atLeastU()) {
        it.getInstallSourceInfo(packageName, Shizuku.getUid())
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

  private fun getElfType(file: File): Int {
    return runCatching {
      getElfParser(file.inputStream()).getEType()
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(ET_NONE)
  }

  private fun getElfParser(input: InputStream): ELFParser {
    return ELFParser(input)
  }

  fun describeSignature(
    context: Context,
    dateFormat: DateFormat,
    signature: Signature
  ): LibStringItem {
    val bytes = signature.toByteArray()
    val certificate = X509Certificate.getInstance(bytes)
    val serialNumber = "0x${certificate.serialNumber.toString(16)}"
    val source = buildString {
      // Signature Version
      append(context.getString(R.string.signature_version))
      append(":v")
      appendLine(certificate.version + 1)
      // Signature Serial Number
      append(context.getString(R.string.signature_serial_number))
      append(":")
      append(certificate.serialNumber)
      append("(")
      append(serialNumber)
      appendLine(")")
      // Signature Issuer
      append(context.getString(R.string.signature_issuer))
      append(":")
      appendLine(certificate.issuerDN)
      // Signature Subject
      append(context.getString(R.string.signature_subject))
      append(":")
      appendLine(certificate.subjectDN)
      // Signature Validity Not Before
      append(context.getString(R.string.signature_validity_not_before))
      append(":")
      appendLine(dateFormat.format(certificate.notBefore))
      // Signature Validity Not After
      append(context.getString(R.string.signature_validity_not_after))
      append(":")
      appendLine(dateFormat.format(certificate.notAfter))
      // Signature Public Key Format
      append(context.getString(R.string.signature_public_key_format))
      append(":")
      appendLine(certificate.publicKey.format)
      append(context.getString(R.string.signature_public_key_algorithm))
      append(":")
      appendLine(certificate.publicKey.algorithm)
      when (val key = certificate.publicKey) {
        is RSAPublicKey -> {
          // Public Key Exponent
          append(context.getString(R.string.signature_public_key_exponent))
          append(":")
          append(key.publicExponent)
          append("(0x")
          append(key.publicExponent.toString(16))
          appendLine(")")
          // Public Key Modulus Size
          append(context.getString(R.string.signature_public_key_modulus_size))
          append(":")
          appendLine(key.modulus.toString(2).length)
          // Public Key Modulus
          append(context.getString(R.string.signature_public_key_modulus))
          append(":")
          appendLine(key.modulus.toByteArray().toHexString(":"))
        }

        is DSAPublicKey -> {
          // Public Key Y
          append(context.getString(R.string.signature_public_key_y))
          append(":")
          appendLine(key.y)
        }

        else -> {
          // Public Key Type
          append(context.getString(R.string.signature_public_key_type))
          append(":")
          appendLine(key.javaClass.simpleName)
        }
      }
      // Signature Algorithm Name
      append(context.getString(R.string.signature_algorithm_name))
      append(":")
      appendLine(certificate.sigAlgName)
      // Signature Algorithm OID
      append(context.getString(R.string.signature_algorithm_oid))
      append(":")
      appendLine(certificate.sigAlgOID)
      // Signature MD5
      append(context.getString(R.string.signature_md5))
      append(":")
      appendLine(bytes.md5(":"))
      // Signature SHA1
      append(context.getString(R.string.signature_sha1))
      append(":")
      appendLine(bytes.sha1(":"))
      // Signature SHA256
      append(context.getString(R.string.signature_sha256))
      append(":")
      appendLine(bytes.sha256(":"))
      // Signature CharString
      append(context.getString(R.string.signature_char_string))
      append(":")
      append(signature.toCharsString())
    }
    return LibStringItem(serialNumber, 0, source, null)
  }

  fun startLaunchAppActivity(context: Context, packageName: String?) {
    if (packageName == null) {
      return
    }
    val launcherActivity: String
    val intent = Intent(Intent.ACTION_MAIN, null)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    val info = PackageManagerCompat.queryIntentActivities(intent, 0)
    launcherActivity = info.getOrNull(0)?.activityInfo?.name.orEmpty()
    val launchIntent = Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setClassName(packageName, launcherActivity)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launchIntent)
  }

  fun getBuildVersionsInfo(packageInfo: PackageInfo?, packageName: String): CharSequence {
    if (packageInfo == null && packageName != Constants.EXAMPLE_PACKAGE) {
      return ""
    }
    val showAndroidVersion =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0
    val showTarget =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_TARGET_API) > 0
    val showMin =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_MIN_API) > 0
    val showCompile =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_COMPILE_API) > 0
    val target = packageInfo?.applicationInfo?.targetSdkVersion ?: Build.VERSION.SDK_INT
    val min = packageInfo?.applicationInfo?.minSdkVersion ?: Build.VERSION.SDK_INT
    val compile = packageInfo?.getCompileSdkVersion() ?: Build.VERSION.SDK_INT

    return buildSpannedString {
      if (OsUtils.atLeastQ() && LocalAppDataSource.apexPackageSet.contains(packageName)) {
        append(", APEX")
      }
      if (showTarget) {
        append(", ")
        scale(0.8f) {
          append("Target: ")
        }
        append(target.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[target]})")
        }
      }

      if (showMin) {
        if (showTarget) {
          append(", ")
        }
        scale(0.8f) {
          append(" Min: ")
        }
        append(min.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[min]})")
        }
      }

      if (showCompile) {
        if (showTarget || showMin) {
          append(", ")
        }
        scale(0.8f) {
          append(" Compile: ")
        }

        append(compile.toString().takeIf { it != "0" } ?: "?")
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[compile] ?: "?"})")
        }
      }
    }
  }
}
