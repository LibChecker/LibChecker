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
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ET_CORE
import com.absinthe.libchecker.annotation.ET_DYN
import com.absinthe.libchecker.annotation.ET_EXEC
import com.absinthe.libchecker.annotation.ET_HIPROC
import com.absinthe.libchecker.annotation.ET_LOPROC
import com.absinthe.libchecker.annotation.ET_NONE
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ET_NOT_SET
import com.absinthe.libchecker.annotation.ET_REL
import com.absinthe.libchecker.annotation.ElfType
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MIPS
import com.absinthe.libchecker.constant.Constants.MIPS64
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.Constants.RISCV32
import com.absinthe.libchecker.constant.Constants.RISCV64
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.dex.FastDexFileFactory
import com.absinthe.libchecker.utils.elf.ELFParser
import com.absinthe.libchecker.utils.elf.ElfInfo
import com.absinthe.libchecker.utils.extensions.ABI_64_BIT
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.ABI_STRING_RES_MAP
import com.absinthe.libchecker.utils.extensions.ABI_VALUE_TO_INSTRUCTION_SET_MAP
import com.absinthe.libchecker.utils.extensions.INSTRUCTION_SET_MAP_TO_ABI_VALUE
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_16_KB
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.extensions.isOverlay
import com.absinthe.libchecker.utils.extensions.isUse32BitAbi
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.libchecker.utils.extensions.orZero
import com.absinthe.libchecker.utils.extensions.sha1
import com.absinthe.libchecker.utils.extensions.sha256
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.extensions.toHexString
import com.absinthe.libchecker.utils.manifest.StaticLibraryReader
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.android.tools.smali.dexlib2.Opcodes
import dev.rikka.tools.refine.Refine
import java.io.File
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.DateFormat
import java.util.zip.ZipEntry
import javax.security.cert.X509Certificate
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.io.input.BufferedFileChannelInputStream
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
    val defaultFlags = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES
    val packageInfo = PackageManagerCompat.getPackageInfo(
      packageName,
      defaultFlags or flag
    ).also {
      it.applicationInfo?.let { ai ->
        if (FreezeUtils.isAppFrozen(ai)) {
          return PackageManagerCompat.getPackageArchiveInfo(
            ai.sourceDir,
            PackageManager.MATCH_DISABLED_COMPONENTS or flag
          )?.apply {
            applicationInfo?.let { appInfo ->
              appInfo.sourceDir = ai.sourceDir
              appInfo.nativeLibraryDir = ai.nativeLibraryDir
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
   * @param specifiedAbi Specify an ABI
   * @return List of LibStringItem
   */
  fun getNativeDirLibs(packageInfo: PackageInfo, specifiedAbi: Int? = null, parseElf: Boolean = false): List<LibStringItem> {
    val nativePath = packageInfo.applicationInfo?.nativeLibraryDir
    val result = mutableListOf<LibStringItem>()

    if (nativePath != null) {
      File(nativePath).listFiles()?.let { files ->
        val libs = files.asSequence()
          .filter { it.isFile && it.extension == "so" }
          .distinctBy { it.name }
          .map {
            val elfParser =
              runCatching { it.inputStream().use { input -> ELFParser(input) } }.getOrNull()
            LibStringItem(
              name = it.name,
              size = FileUtils.getFileSize(it),
              elfInfo = ElfInfo(
                elfParser?.getEType() ?: ET_NOT_ELF,
                elfParser?.getMinPageSize() ?: -1
              )
            )
          }
        result.addAll(libs)
      }
    }

    if (result.isEmpty()) {
      val abi =
        specifiedAbi ?: runCatching { getAbi(packageInfo) }.getOrNull() ?: return emptyList()

      if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) {
        return emptyList()
      }
      val sourceDir = ABI_STRING_MAP[abi % MULTI_ARCH]
      val libs = getSourceLibs(
        packageInfo = packageInfo,
        specifiedAbi = abi,
        includeNativeLibsDir = false,
        parseElf = parseElf
      )[sourceDir] ?: emptyList()
      result.addAll(libs)
    }

    return result.distinctBy { it.name }
  }

  /**
   * Get native libraries of an app from source path
   * @param packageInfo PackageInfo
   * @param specifiedAbi Specify an ABI
   * @param includeNativeLibsDir Whether include libs from native libs directory
   * @return List of LibStringItem
   */
  fun getSourceLibs(
    packageInfo: PackageInfo,
    specifiedAbi: Int? = null,
    includeNativeLibsDir: Boolean = true,
    parseElf: Boolean
  ): Map<String, List<LibStringItem>> {
    if (specifiedAbi == ERROR || specifiedAbi == NO_LIBS || specifiedAbi == OVERLAY) return emptyMap()
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return emptyMap()
    val file = File(sourceDir)
    var map = getApkFileLibs(file, specifiedAbi, parseElf)
    if (map.isEmpty()) {
      map = getSplitLibs(packageInfo, specifiedAbi, parseElf)
    }
    if (map.isEmpty() && includeNativeLibsDir) {
      val abi = specifiedAbi ?: getAbi(packageInfo)
      if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) return emptyMap()
      val libs = getNativeDirLibs(packageInfo, specifiedAbi, parseElf).toMutableList()
      map = mapOf(ABI_STRING_MAP[abi % MULTI_ARCH]!! to libs)
    }
    return map
  }

  /**
   * Get native libraries of an app from split apk
   * @param packageInfo PackageInfo
   * @return List of LibStringItem
   */
  private fun getSplitLibs(packageInfo: PackageInfo, specifiedAbi: Int? = null, parseElf: Boolean): Map<String, MutableList<LibStringItem>> {
    val splitList = getSplitsSourceDir(packageInfo)
    if (splitList.isNullOrEmpty()) {
      return emptyMap()
    }

    val map = mutableMapOf<String, MutableList<LibStringItem>>()
    splitList.filter {
      val fileName = it.split(File.separator).last()
      val specifiedAvailable = specifiedAbi != null && fileName.contains(ABI_VALUE_TO_INSTRUCTION_SET_MAP[specifiedAbi].toString())
      val isAbiSplitFile = specifiedAbi == null && INSTRUCTION_SET_MAP_TO_ABI_VALUE.keys.any { key -> fileName.contains(key) }
      specifiedAvailable || isAbiSplitFile
    }.forEach { split ->
      val splitMap = getApkFileLibs(File(split), parseElf = parseElf)
      for ((key, newList) in splitMap) {
        map.merge(key, newList) { existingList, _ ->
          existingList.apply { addAll(newList) }
        }
      }
    }

    return map
  }

  private val regex_splits by lazy { Regex("split_(delivery|config)\\.?(.*)\\.apk") }

  /**
   * Get split apks dirs
   * @param packageInfo PackageInfo
   * @return List of split apks dirs
   */
  fun getSplitsSourceDir(packageInfo: PackageInfo): Array<String>? {
    val ai = packageInfo.applicationInfo ?: return null
    if (FreezeUtils.isAppFrozen(ai)) {
      File(ai.sourceDir).parentFile?.takeIf { it.exists() }?.let { files ->
        return files.listFiles { file -> file.name.matches(regex_splits) }
          ?.map { it.absolutePath }
          ?.toTypedArray()
      }
    }
    return ai.splitSourceDirs
  }

  private const val ENABLE_GET_APK_FILE_LIBS_LOG = false

  private fun getApkFileLibs(file: File, specifiedAbi: Int? = null, parseElf: Boolean): Map<String, MutableList<LibStringItem>> {
    if (file.exists().not()) {
      return emptyMap()
    }
    if (!parseElf) {
      return getApkFileLibsWithoutParsingElf(file, specifiedAbi)
    }

    val libDir = "lib"
    val assetsDir = "assets"
    val sourceDir = specifiedAbi?.let { libDir + File.separator + ABI_STRING_MAP[it % MULTI_ARCH] }
    val map = mutableMapOf<String, MutableList<LibStringItem>>()
    val pendingEntryDirMap = mutableMapOf<String, Int>()
    val timeRecorder = TimeRecorder()

    if (ENABLE_GET_APK_FILE_LIBS_LOG) timeRecorder.start()
    ZipFileCompat(file).use { zipFile ->
      zipFile.getZipEntries()
        .asSequence()
        .filter { it.name.endsWith(".so") }
        .forEach {
          if (it.name.startsWith(libDir + File.separator)) {
            val split = it.name.split(File.separator)
            if (split.size >= 2) {
              val dir = split[1]
              pendingEntryDirMap[dir] = pendingEntryDirMap.getOrPut(dir) { 0 } + 1
            }
          } else if (it.name.startsWith(assetsDir + File.separator)) {
            pendingEntryDirMap[assetsDir] = pendingEntryDirMap.getOrPut(assetsDir) { 0 } + 1
          }
        }
    }
    if (ENABLE_GET_APK_FILE_LIBS_LOG) {
      timeRecorder.end()
      Timber.d("${file.absolutePath} Check zipFile cost: $timeRecorder")
      Timber.d("${file.absolutePath} Check zipFile pendingEntryDirMap: $pendingEntryDirMap")
    }

    if (ENABLE_GET_APK_FILE_LIBS_LOG) timeRecorder.start()
    val bufferedIS = BufferedFileChannelInputStream.Builder()
      .setFile(file)
      .get()
    bufferedIS.use { input ->
      ZipArchiveInputStream(input).use { zipInput ->
        var entry = zipInput.nextEntry
        while (entry != null) {
          try {
            if (!entry.name.endsWith(".so") || (sourceDir != null && !entry.name.startsWith(sourceDir))) {
              if (pendingEntryDirMap.all { it.value == 0 }) {
                // Optimize for skipping unused entries, since sorted by name
                if (ENABLE_GET_APK_FILE_LIBS_LOG) Timber.d("${file.absolutePath} Skip at entry: ${entry.name}")
                break
              }
              continue
            }
            val pathFirst = entry.name.split(File.separator).first()
            val dir = STRING_ABI_MAP.keys.find { entry.name.startsWith(libDir + File.separator + it) }
              ?: if (pendingEntryDirMap.keys.contains(assetsDir) && pathFirst == assetsDir) assetsDir else continue
            val count = pendingEntryDirMap.getOrPut(dir) { 0 }
            if (count > 0) {
              pendingEntryDirMap[dir] = count - 1
            }
            val currentEntryUncompressedAndNot16KB = entry.method == ZipEntry.STORED && zipInput.bytesRead % PAGE_SIZE_16_KB != 0L
            val elfParser = runCatching { ELFParser(zipInput) }.getOrNull()
            val libSize = entry.size.takeIf { it != ZipArchiveEntry.SIZE_UNKNOWN } ?: (FileUtils.getEntrySize(zipInput) + (elfParser?.readBytes.orZero()))
            val item = LibStringItem(
              name = entry.name.split(File.separator).last(),
              size = libSize,
              elfInfo = ElfInfo(
                elfParser?.getEType() ?: ET_NOT_ELF,
                elfParser?.getMinPageSize() ?: -1,
                currentEntryUncompressedAndNot16KB
              )
            )
            map.getOrPut(dir) { mutableListOf() }.add(item)
          } finally {
            runCatching {
              entry = zipInput.nextEntry
            }.onFailure {
              Timber.e(it, "${file.absolutePath} Get apk file libs error: ")
              entry = null
            }
          }
        }
      }
      if (ENABLE_GET_APK_FILE_LIBS_LOG) {
        timeRecorder.end()
        Timber.d("${file.absolutePath} Get apk file libs cost: $timeRecorder")
      }
    }
    return map
  }

  private fun getApkFileLibsWithoutParsingElf(file: File, specifiedAbi: Int? = null): Map<String, MutableList<LibStringItem>> {
    if (file.exists().not()) {
      return emptyMap()
    }
    val libDir = "lib"
    val assetsDir = "assets"
    val sourceDir = specifiedAbi?.let { libDir + File.separator + ABI_STRING_MAP[it % MULTI_ARCH] }
    val map = mutableMapOf<String, MutableList<LibStringItem>>()

    ZipFileCompat(file).use { zipFile ->
      zipFile.getZipEntries()
        .asSequence()
        .filter {
          it.isDirectory.not() && it.name.endsWith(".so") && (sourceDir == null || it.name.startsWith(sourceDir))
        }
        .forEach { entry ->
          val pathFirst = entry.name.split(File.separator).first()
          val dir = STRING_ABI_MAP.keys.find { entry.name.startsWith(libDir + File.separator + it) }
            ?: if (pathFirst == assetsDir) assetsDir else return@forEach
          val item = LibStringItem(
            name = entry.name.split(File.separator).last(),
            size = entry.size,
            elfInfo = ElfInfo()
          )
          map.getOrPut(dir) { mutableListOf() }.add(item)
        }
    }

    return map
  }

  const val STATIC_LIBRARY_SOURCE_PREFIX = "[Path] "
  const val VERSION_CODE_PREFIX = "[Version Code] "

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
    val ai = packageInfo.applicationInfo ?: return emptyList()
    val appResources by lazy { SystemServices.packageManager.getResourcesForApplication(ai) }
    ai.metaData?.let {
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
    val abiSet = mutableSetOf<Int>()

    if (file.exists().not()) {
      Timber.w("File not exists: ${file.absolutePath}")
      return abiSet
    }

    if (packageInfo.isOverlay()) {
      abiSet.add(OVERLAY)
      return abiSet
    }

    ZipFileCompat(file).use { zipFile ->
      return runCatching {
        val libDirPrefix = "lib" + File.separator
        var encounteredLibsDir = false
        val entries = zipFile.getZipEntries()

        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()

          if (entry.isDirectory) {
            continue
          }

          val elementName = entry.name

          if (elementName.startsWith(libDirPrefix)) {
            STRING_ABI_MAP.forEach { (string, abi) ->
              if (elementName.startsWith("$libDirPrefix$string${File.separator}")) {
                encounteredLibsDir = true
                if (Build.SUPPORTED_ABIS.contains(string) || ignoreArch) {
                  abiSet.add(abi)
                }
                return@forEach
              }
            }
          } else if (encounteredLibsDir) {
            break
          }
        }

        if (abiSet.isEmpty()) {
          if (!isApk && packageInfo.applicationInfo?.nativeLibraryDir != null) {
            abiSet.addAll(getAbiListByNativeDir(packageInfo.applicationInfo!!.nativeLibraryDir))
          } else if (packageInfo.applicationInfo!!.splitSourceDirs != null) {
            abiSet.addAll(getAbiListBySplitApks(packageInfo.applicationInfo!!.splitSourceDirs!!))
          }

          if (abiSet.isEmpty()) {
            abiSet.add(NO_LIBS)
          }
        }
        return abiSet
      }.onFailure {
        Timber.e(it)
        abiSet.clear()
        abiSet.add(ERROR)
        return abiSet
      }.getOrDefault(abiSet)
    }
  }

  /**
   * Get ABI type of an app from native path
   * @param nativePath Native path of the app
   * @return ABI type
   */
  private fun getAbiListByNativeDir(nativePath: String): MutableSet<Int> {
    val file = File(nativePath.substring(0, nativePath.lastIndexOf(File.separator)))
    val abis = mutableSetOf<Int>()

    val fileList = file.listFiles() ?: return mutableSetOf()

    fileList.asSequence()
      .forEach {
        if (it.isDirectory) {
          INSTRUCTION_SET_MAP_TO_ABI_VALUE[it.name]?.let { abi ->
            abis.add(abi)
          }
        }
      }

    return abis
  }

  private fun getAbiListBySplitApks(splitSource: Array<String>): Set<Int> {
    return splitSource.filter { source -> STRING_ABI_MAP.keys.any { source.contains(it) } }
      .map { source ->
        val abiString = source.substringAfterLast(File.separator)
          .removePrefix("split_config.")
          .removeSuffix(".apk")
        STRING_ABI_MAP[abiString] ?: ERROR
      }
      .filter { it != ERROR }
      .toSet()
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
    val applicationInfo: ApplicationInfo = packageInfo.applicationInfo ?: return ERROR

    if (packageInfo.isArchivedPackage()) {
      return NO_LIBS
    }

    if (packageInfo.isOverlay()) {
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

    val primaryCpuAbi = Refine.unsafeCast<ApplicationInfoHidden>(applicationInfo).primaryCpuAbi
    var abi = STRING_ABI_MAP[primaryCpuAbi] ?: let {
      val supportedAbiSet = mutableSetOf<Int>()
      realAbiSet.forEach {
        if (Build.SUPPORTED_ABIS.contains(ABI_STRING_MAP[it])) {
          supportedAbiSet.add(it)
        }
      }
      if (use32bitAbi) {
        supportedAbiSet.removeAll(ABI_64_BIT)
      }
      when {
        supportedAbiSet.contains(ARMV8) -> ARMV8
        supportedAbiSet.contains(ARMV7) -> ARMV7
        supportedAbiSet.contains(ARMV5) -> ARMV5
        supportedAbiSet.contains(X86_64) -> X86_64
        supportedAbiSet.contains(X86) -> X86
        supportedAbiSet.contains(MIPS64) -> MIPS64
        supportedAbiSet.contains(MIPS) -> MIPS
        supportedAbiSet.contains(RISCV64) -> RISCV64
        supportedAbiSet.contains(RISCV32) -> RISCV32
        else -> ERROR
      }
    }

    if (multiArch) {
      abi += MULTI_ARCH
    }
    return abi
  }

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
    return when (type) {
      OVERLAY -> R.drawable.ic_abi_label_no_libs
      ERROR -> 0
      else -> if (isAbi64Bit(type % MULTI_ARCH)) R.drawable.ic_abi_label_64bit else R.drawable.ic_abi_label_32bit
    }
  }

  fun isAbi64Bit(abi: Int): Boolean {
    if (abi == NO_LIBS) {
      return Process.is64Bit()
    }
    return abi in ABI_64_BIT
  }

  /**
   * Format size number to string
   * @param item LibStringItem
   * @return String of size number (100KB)
   */
  fun sizeToString(
    context: Context,
    item: LibStringItem
  ): String {
    val source = item.source?.let { "[${item.source}]" }.orEmpty()
    return "(${item.size.sizeToString(context, showBytes = false)}) $source"
  }

  /**
   * Format ELF type to string
   * @param type ELF type
   * @return String of ELF type
   */
  fun elfTypeToString(@ElfType type: Int): String {
    return when (type) {
      ET_NOT_SET -> ""
      ET_NOT_ELF -> "Broken ELF"
      ET_NONE -> "No File Type"
      ET_REL -> "Relocatable File"
      ET_EXEC -> "Executable File"
      ET_DYN -> "Shared Object File"
      ET_CORE -> "Core File"
      ET_LOPROC -> "Processor-specific"
      ET_HIPROC -> "Processor-specific"
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
   * @param pi PackageInfo of the app
   * @return List of LibStringItem
   */
  fun getDexList(pi: PackageInfo): Collection<LibStringItem> {
    throw RuntimeException("Not implemented")
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

  fun getLauncherActivity(packageName: String): String {
    val intent = Intent(Intent.ACTION_MAIN, null)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    val info = PackageManagerCompat.queryIntentActivities(intent, 0)
    return info.getOrNull(0)?.activityInfo?.name.orEmpty()
  }

  fun startLaunchAppActivity(context: Context, packageName: String?) {
    if (packageName == null) {
      return
    }
    val launcherActivity = getLauncherActivity(packageName)
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

  fun hasNoNativeLibs(abi: Int): Boolean {
    return abi == OVERLAY || (abi % MULTI_ARCH) == NO_LIBS
  }
}
