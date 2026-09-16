package com.absinthe.libchecker.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfoHidden
import android.content.pm.ComponentInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Process
import android.os.Trace
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
import com.absinthe.libchecker.compat.IZipFile
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
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.StatefulComponent
import com.absinthe.libchecker.utils.apk.ZipDataOffsetReader
import com.absinthe.libchecker.utils.dex.StreamingDexClassScanner
import com.absinthe.libchecker.utils.elf.ElfInfo
import com.absinthe.libchecker.utils.elf.ElfParser
import com.absinthe.libchecker.utils.extensions.ABI_64_BIT
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.ABI_STRING_RES_MAP
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
import com.absinthe.libchecker.utils.extensions.sha1
import com.absinthe.libchecker.utils.extensions.sha256
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.extensions.toHexString
import com.absinthe.libchecker.utils.manifest.StaticLibraryReader
import com.absinthe.libraries.utils.manager.TimeRecorder
import dev.rikka.tools.refine.Refine
import java.io.File
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.DateFormat
import java.util.zip.ZipEntry
import javax.security.cert.X509Certificate
import kotlinx.coroutines.CancellationException
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
   * Get native libraries of an app
   * @param packageInfo PackageInfo
   * @param specifiedAbi Specify an ABI
   * @return List of LibStringItem
   */
  fun getNativeDirLibs(packageInfo: PackageInfo, specifiedAbi: Int? = null, parseElf: Boolean = false, checkCancelled: () -> Unit = {}): List<LibStringItem> {
    checkCancelled()
    val nativePath = packageInfo.applicationInfo?.nativeLibraryDir
    val result = mutableListOf<LibStringItem>()

    if (nativePath != null) {
      File(nativePath).listFiles()?.let { files ->
        val libs = files.asSequence()
          .filter {
            checkCancelled()
            it.isFile && it.name.endsWith(".so")
          }
          .distinctBy { it.name }
          .map {
            LibStringItem(
              name = it.name,
              size = FileUtils.getFileSize(it),
              elfInfo = parseNativeDirElfInfo(it, parseElf, checkCancelled),
              source = it.path
            )
          }
        result.addAll(libs)
      }
    }

    if (result.isEmpty()) {
      if (specifiedAbi == null && !parseElf) {
        return getDefaultAbiNativeLibs(packageInfo, checkCancelled).distinctBy { it.name }
      }
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
        parseElf = parseElf,
        checkCancelled = checkCancelled
      )[sourceDir] ?: emptyList()
      result.addAll(libs)
    }

    return result.distinctBy { it.name }
  }

  // ABI discovery already visits every base APK entry. Keep only the matching
  // library records so the default, non-ELF path does not enumerate that ZIP twice.
  private fun getDefaultAbiNativeLibs(packageInfo: PackageInfo, checkCancelled: () -> Unit): List<LibStringItem> {
    val applicationInfo = packageInfo.applicationInfo ?: return emptyList()
    val sourceDir = applicationInfo.sourceDir ?: return emptyList()
    val baseLibs = mutableMapOf<String, MutableList<LibStringItem>>()
    val abiSet = runCatching {
      if (packageInfo.isArchivedPackage() || packageInfo.isOverlay()) return emptyList()
      scanAbiSet(
        file = File(sourceDir),
        packageInfo = packageInfo,
        isApk = false,
        ignoreArch = true,
        checkCancelled = checkCancelled
      ) { entry, abiName ->
        baseLibs.getOrPut(abiName) { mutableListOf() }.add(
          LibStringItem(name = getApkLibEntryFileName(entry.name), size = entry.size, elfInfo = ElfInfo())
        )
      }
    }.getOrElse {
      if (it is CancellationException) throw it
      return emptyList()
    }
    checkCancelled()
    val abi = runCatching { getAbi(packageInfo, abiSet = abiSet) }.getOrNull() ?: return emptyList()
    if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) return emptyList()
    val abiName = ABI_STRING_MAP[abi % MULTI_ARCH]
    // A failed scan may have collected only part of the archive. Preserve the
    // existing reader/fallback behavior instead of returning partial records.
    if (ERROR in abiSet) {
      baseLibs.clear()
      return getSourceLibs(packageInfo, abi, includeNativeLibsDir = false, parseElf = false, checkCancelled = checkCancelled)[abiName].orEmpty()
    }
    return baseLibs[abiName].orEmpty() +
      getSplitLibs(packageInfo, abi, parseElf = false, checkCancelled = checkCancelled)[abiName].orEmpty()
  }

  internal fun parseNativeDirElfInfo(file: File, parseElf: Boolean, checkCancelled: () -> Unit = {}): ElfInfo {
    checkCancelled()
    if (!parseElf) {
      return ElfInfo()
    }
    return runCatching {
      ElfParser(file, checkCancelled).use { parser ->
        parser.parseHeader()
        ElfInfo(parser.getEType(), parser.getMinPageSize())
      }
    }.getOrElse {
      if (it is CancellationException) throw it
      ElfInfo(ET_NOT_ELF, -1)
    }
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
    parseElf: Boolean,
    parseElfForAbi: Int? = null,
    checkCancelled: () -> Unit = {}
  ): Map<String, List<LibStringItem>> {
    if (specifiedAbi == ERROR || specifiedAbi == NO_LIBS || specifiedAbi == OVERLAY) return emptyMap()
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return emptyMap()
    val file = File(sourceDir)
    val map = getApkFileLibs(file, specifiedAbi, parseElf, parseElfForAbi, checkCancelled).toMutableMap()
    for ((abi, libs) in getSplitLibs(packageInfo, specifiedAbi, parseElf, parseElfForAbi, checkCancelled)) {
      map.getOrPut(abi) { mutableListOf() }.addAll(libs)
    }
    if (map.isEmpty() && includeNativeLibsDir) {
      val abi = specifiedAbi ?: getAbi(packageInfo)
      if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) return emptyMap()
      val shouldParseNativeDir = parseElf || parseElfForAbi?.let {
        it % MULTI_ARCH == abi % MULTI_ARCH
      } == true
      val libs = getNativeDirLibs(packageInfo, specifiedAbi, shouldParseNativeDir, checkCancelled).toMutableList()
      map += mapOf(ABI_STRING_MAP[abi % MULTI_ARCH]!! to libs)
    }
    return map
  }

  /**
   * Get native libraries of an app from split apk
   * @param packageInfo PackageInfo
   * @return List of LibStringItem
   */
  private fun getSplitLibs(
    packageInfo: PackageInfo,
    specifiedAbi: Int? = null,
    parseElf: Boolean,
    parseElfForAbi: Int? = null,
    checkCancelled: () -> Unit = {}
  ): Map<String, MutableList<LibStringItem>> {
    val splitList = getSplitsSourceDir(packageInfo)
    if (splitList.isNullOrEmpty()) {
      return emptyMap()
    }

    val map = mutableMapOf<String, MutableList<LibStringItem>>()
    splitList.forEach { split ->
      checkCancelled()
      val splitMap = getApkFileLibs(
        file = File(split),
        specifiedAbi = specifiedAbi,
        parseElf = parseElf,
        parseElfForAbi = parseElfForAbi,
        checkCancelled = checkCancelled
      )
      for ((key, newList) in splitMap) {
        map.merge(key, newList) { existingList, _ ->
          existingList.apply { addAll(newList) }
        }
      }
    }

    return map
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

  private const val ENABLE_GET_APK_FILE_LIBS_LOG = true
  private const val APK_ENTRY_SEPARATOR = '/'
  private const val TRACE_APK_LIBS_DATA_OFFSET = "LC PackageUtils apkLibsDataOffset"
  private const val TRACE_APK_LIBS_MATCH_ENTRIES = "LC PackageUtils apkLibsMatchEntries"
  private const val TRACE_APK_LIBS_OPEN_ZIP = "LC PackageUtils apkLibsOpenZip"
  private const val TRACE_APK_LIBS_PARSE_ELF = "LC PackageUtils apkLibsParseElf"
  private const val TRACE_FIND_DEX_CLASSES = "LC PackageUtils findDexClasses"

  private fun getApkFileLibs(
    file: File,
    specifiedAbi: Int? = null,
    parseElf: Boolean,
    parseElfForAbi: Int? = null,
    checkCancelled: () -> Unit = {}
  ): Map<String, MutableList<LibStringItem>> {
    checkCancelled()
    if (file.exists().not() || file.canRead().not()) {
      return emptyMap()
    }
    if (!parseElf && parseElfForAbi == null) {
      return getApkFileLibsWithoutParsingElf(file, specifiedAbi, checkCancelled)
    }

    val libDir = "lib"
    val assetsDir = "assets"
    val sourceDir = specifiedAbi?.let {
      libDir + File.separator + ABI_STRING_MAP[it % MULTI_ARCH] + File.separator
    }
    val parseElfSourceDir = parseElfForAbi?.let {
      libDir + File.separator + ABI_STRING_MAP[it % MULTI_ARCH] + File.separator
    }
    val map = mutableMapOf<String, MutableList<LibStringItem>>()
    val timeRecorder = TimeRecorder()

    if (ENABLE_GET_APK_FILE_LIBS_LOG) timeRecorder.start()
    try {
      tracePackageUtilsSection(TRACE_APK_LIBS_OPEN_ZIP) { ZipFileCompat(file) }.use { zipFile ->
        val nativeEntries = collectNativeZipEntries(
          zipFile = zipFile,
          sourceDir = sourceDir,
          parseElf = parseElf,
          parseElfSourceDir = parseElfSourceDir,
          checkCancelled = checkCancelled
        )
        val storedEntryOffsets by lazy {
          loadStoredEntryOffsets(file, nativeEntries.storedEntryNames, checkCancelled)
        }
        nativeEntries.entries.forEach { entry ->
          checkCancelled()
          val entryName = entry.name
          val dir = getApkLibEntryDir(entryName, libDir, assetsDir) ?: return@forEach
          val shouldParseElf = parseElf || parseElfSourceDir?.let { entryName.startsWith(it) } == true

          val currentEntryZipAlignment = if (shouldParseElf && entry.method == ZipEntry.STORED) {
            getZipAlignment(storedEntryOffsets[entryName] ?: -1L)
          } else {
            -1
          }
          val elfInfo = if (shouldParseElf) {
            tracePackageUtilsSection(TRACE_APK_LIBS_PARSE_ELF) {
              runCatching {
                ElfParser(zipFile.getInputStream(entry), checkCancelled).use { parser ->
                  parser.parseHeader()
                  ElfInfo(
                    parser.getEType(),
                    parser.getMinPageSize(),
                    zipAlignment = currentEntryZipAlignment
                  )
                }
              }.getOrElse {
                if (it is CancellationException) throw it
                ElfInfo(
                  ET_NOT_ELF,
                  -1,
                  zipAlignment = currentEntryZipAlignment
                )
              }
            }
          } else {
            ElfInfo()
          }

          val item = LibStringItem(
            name = getApkLibEntryFileName(entryName),
            size = entry.size,
            elfInfo = elfInfo,
            source = entryName,
            process = file.name
          )
          map.getOrPut(dir) { mutableListOf() }.add(item)
        }
      }
    } catch (e: OutOfMemoryError) {
      logApkFileLibsFailure(file, parseElf = true, e)
      return emptyMap()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logApkFileLibsFailure(file, parseElf = true, e)
      return getApkFileLibsWithoutParsingElf(file, specifiedAbi, checkCancelled)
    }
    if (ENABLE_GET_APK_FILE_LIBS_LOG) {
      timeRecorder.end()
      Timber.d("${file.absolutePath} Check zipFile cost: $timeRecorder")
    }
    return map
  }

  private fun collectNativeZipEntries(
    zipFile: IZipFile,
    sourceDir: String?,
    parseElf: Boolean,
    parseElfSourceDir: String?,
    checkCancelled: () -> Unit
  ): NativeZipEntries {
    return tracePackageUtilsSection(TRACE_APK_LIBS_MATCH_ENTRIES) {
      val entries = mutableListOf<ZipEntry>()
      val storedEntryNames = mutableSetOf<String>()
      val zipEntries = zipFile.getZipEntries()
      while (zipEntries.hasMoreElements()) {
        checkCancelled()
        val entry = zipEntries.nextElement()
        val entryName = entry.name
        if (
          !entry.isDirectory &&
          entryName.endsWith(".so") &&
          (sourceDir == null || entryName.startsWith(sourceDir))
        ) {
          entries.add(entry)
          val shouldParseElf = parseElf || parseElfSourceDir?.let { entryName.startsWith(it) } == true
          if (shouldParseElf && entry.method == ZipEntry.STORED) {
            storedEntryNames.add(entryName)
          }
        }
      }
      NativeZipEntries(entries, storedEntryNames)
    }
  }

  private fun getZipAlignment(offset: Long): Long {
    if (offset <= 0L) {
      return -1
    }
    return java.lang.Long.lowestOneBit(offset)
  }

  private fun loadStoredEntryOffsets(file: File, entryNames: Set<String>, checkCancelled: () -> Unit): Map<String, Long> {
    return tracePackageUtilsSection(TRACE_APK_LIBS_DATA_OFFSET) {
      ZipDataOffsetReader.read(file, entryNames, checkCancelled).also { offsets ->
        check(offsets.keys.containsAll(entryNames)) {
          "ZIP data offsets are incomplete for ${file.absolutePath}"
        }
      }
    }
  }

  private data class NativeZipEntries(
    val entries: List<ZipEntry>,
    val storedEntryNames: Set<String>
  )

  private inline fun <T> tracePackageUtilsSection(sectionName: String, block: () -> T): T {
    Trace.beginSection(sectionName)
    return try {
      block()
    } finally {
      Trace.endSection()
    }
  }

  private fun getApkFileLibsWithoutParsingElf(file: File, specifiedAbi: Int? = null, checkCancelled: () -> Unit = {}): Map<String, MutableList<LibStringItem>> {
    checkCancelled()
    if (file.exists().not()) {
      return emptyMap()
    }
    val libDir = "lib"
    val assetsDir = "assets"
    val sourceDir = specifiedAbi?.let { libDir + File.separator + ABI_STRING_MAP[it % MULTI_ARCH] }
    val map = mutableMapOf<String, MutableList<LibStringItem>>()

    try {
      ZipFileCompat(file).use { zipFile ->
        zipFile.getZipEntries()
          .asSequence()
          .filter {
            checkCancelled()
            it.isDirectory.not() && it.name.endsWith(".so") && (sourceDir == null || it.name.startsWith(sourceDir))
          }
          .forEach { entry ->
            val entryName = entry.name
            val dir = getApkLibEntryDir(entryName, libDir, assetsDir) ?: return@forEach
            val item = LibStringItem(
              name = getApkLibEntryFileName(entryName),
              size = entry.size,
              elfInfo = ElfInfo()
            )
            map.getOrPut(dir) { mutableListOf() }.add(item)
          }
      }
    } catch (e: OutOfMemoryError) {
      logApkFileLibsFailure(file, parseElf = false, e)
      return emptyMap()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logApkFileLibsFailure(file, parseElf = false, e)
      return emptyMap()
    }

    return map
  }

  private fun getApkLibEntryDir(
    entryName: String,
    libDir: String,
    assetsDir: String
  ): String? {
    val firstSeparator = entryName.indexOf(APK_ENTRY_SEPARATOR)
    val firstSegment = if (firstSeparator >= 0) {
      entryName.substring(0, firstSeparator)
    } else {
      entryName
    }

    return when (firstSegment) {
      assetsDir -> assetsDir

      libDir -> {
        val secondSeparator = entryName.indexOf(APK_ENTRY_SEPARATOR, firstSeparator + 1)
        val secondSegment = if (secondSeparator >= 0) {
          entryName.substring(firstSeparator + 1, secondSeparator)
        } else {
          entryName.substring(firstSeparator + 1)
        }
        secondSegment.takeIf { STRING_ABI_MAP.containsKey(it) }
      }

      else -> null
    }
  }

  private fun getApkLibEntryFileName(entryName: String): String {
    val lastSeparator = entryName.lastIndexOf(APK_ENTRY_SEPARATOR)
    return if (lastSeparator >= 0) {
      entryName.substring(lastSeparator + 1)
    } else {
      entryName
    }
  }

  private fun logApkFileLibsFailure(file: File, parseElf: Boolean, throwable: Throwable) {
    Timber.w(throwable, "Failed to parse native libs from ${file.absolutePath}, parseElf=$parseElf")
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

  private val KOTLIN_DEX_PATTERNS = listOf(
    "kotlin.*".toClassDefType(),
    "kotlinx.*".toClassDefType()
  )
  private val UNKNOWN_ABI_RES = listOf(R.string.unknown)

  /**
   * Check if an app uses Kotlin language from classes.dex
   * @param file APK file of the app
   * @return true if it uses Kotlin language
   */
  fun isKotlinUsedInClassDex(file: File): Boolean {
    return findDexClasses(
      file,
      KOTLIN_DEX_PATTERNS,
      hasAny = true
    ).isNotEmpty() || hasKotlinRuntimeEvidenceInClassDex(file)
  }

  /** Detects Kotlin runtime traces that remain after R8 renames and repackages class definitions. */
  internal fun hasKotlinRuntimeEvidenceInClassDex(file: File): Boolean {
    return runCatching {
      ZipFileCompat(file).use { hasKotlinRuntimeEvidenceInClassDex(it) }
    }.getOrDefault(false)
  }

  internal fun hasKotlinRuntimeEvidenceInClassDex(zipFile: IZipFile, checkCancelled: () -> Unit = {}): Boolean {
    val foundMarkers = linkedSetOf<String>()
    var remainingScanBytes = MAX_KOTLIN_RUNTIME_SCAN_BYTES
    return runCatching {
      zipFile.getZipEntries().asSequence()
        .filter {
          checkCancelled()
          isDexEntryName(it.name)
        }
        .forEach { entry ->
          checkCancelled()
          if (remainingScanBytes <= 0) return@runCatching false
          val remainingMarkers = KOTLIN_RUNTIME_STRING_MARKERS.filterNot(foundMarkers::contains)
          val scanResult = zipFile.getInputStream(entry).use { inputStream ->
            StreamingDexClassScanner.findStringMatches(
              inputStream = inputStream,
              stringPatterns = remainingMarkers,
              stopAfterMatches = KOTLIN_RUNTIME_STRING_THRESHOLD - foundMarkers.size,
              entrySize = entry.size.takeIf { it >= 0 },
              maxScanBytes = remainingScanBytes,
              checkCancelled = checkCancelled
            )
          }
          remainingScanBytes -= scanResult.bytesRead
          foundMarkers += scanResult.matches
          if (foundMarkers.size >= KOTLIN_RUNTIME_STRING_THRESHOLD) {
            return@runCatching true
          }
        }
      false
    }.getOrElse {
      if (it is java.util.concurrent.CancellationException) throw it
      false
    }
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
    return scanAbiSet(file, packageInfo, isApk, ignoreArch)
  }

  private fun scanAbiSet(
    file: File,
    packageInfo: PackageInfo,
    isApk: Boolean,
    ignoreArch: Boolean,
    checkCancelled: () -> Unit = {},
    onNativeEntry: ((ZipEntry, String) -> Unit)? = null
  ): Set<Int> {
    checkCancelled()
    val abiSet = mutableSetOf<Int>()

    if (file.exists().not()) {
      Timber.w("File not exists: ${file.absolutePath}")
      return abiSet
    }

    if (packageInfo.isOverlay()) {
      abiSet.add(OVERLAY)
      return abiSet
    }

    return runCatching {
      val libDirPrefix = "lib${File.separator}"

      ZipFileCompat(file).use { zipFile ->
        zipFile.getZipEntries()
          .asSequence()
          .filter {
            checkCancelled()
            !it.isDirectory && it.name.startsWith(libDirPrefix) && it.name.endsWith(".so")
          }
          .mapNotNull { entry ->
            val abiName = entry.name.removePrefix(libDirPrefix)
              .substringBefore('/')
              .substringBefore(File.separatorChar)
            val abi = STRING_ABI_MAP[abiName]
            if (abi != null && (ignoreArch || Build.SUPPORTED_ABIS.contains(abiName))) {
              onNativeEntry?.invoke(entry, abiName)
              abi
            } else {
              null
            }
          }
          .toCollection(abiSet)
      }

      if (abiSet.isEmpty()) {
        packageInfo.applicationInfo?.let { ai ->
          if (!isApk && ai.nativeLibraryDir != null && File(ai.nativeLibraryDir).exists()) {
            abiSet.addAll(getAbiListByNativeDir(ai.nativeLibraryDir))
          } else if (ai.splitSourceDirs != null) {
            abiSet.addAll(getAbiListBySplitApks(ai.splitSourceDirs ?: emptyArray()))
          }
        }
      }
      if (abiSet.isEmpty()) {
        abiSet.add(NO_LIBS)
      }

      abiSet
    }.getOrElse {
      if (it is CancellationException) throw it
      Timber.e(it)
      mutableSetOf(ERROR)
    }
  }

  /**
   * Get ABI type of an app from native path
   * @param nativePath Native path of the app
   * @return ABI type
   */
  private fun getAbiListByNativeDir(nativePath: String): MutableSet<Int> {
    val file = File(nativePath.substring(0, nativePath.lastIndexOf(File.separator)))
    return file.listFiles()
      ?.asSequence()
      ?.filter { it.isDirectory }
      ?.mapNotNullTo(mutableSetOf()) { INSTRUCTION_SET_MAP_TO_ABI_VALUE[it.name] }
      ?: mutableSetOf()
  }

  private fun getAbiListBySplitApks(splitSource: Array<String>): Set<Int> {
    return splitSource.mapNotNullTo(mutableSetOf()) { source ->
      val fileName = source.substringAfterLast(File.separator)
      if (fileName.startsWith("split_config.") && fileName.endsWith(".apk")) {
        val abiString = fileName.substring("split_config.".length, fileName.length - 4)
        STRING_ABI_MAP[abiString]
      } else {
        null
      }
    }
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
      Timber.e("sourceDir is null: ${packageInfo.packageName}")
      return ERROR
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
      val supportedAbiSet = realAbiSet.filter { abi ->
        Build.SUPPORTED_ABIS.contains(ABI_STRING_MAP[abi])
      }.toMutableSet()
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
      ABI_STRING_RES_MAP[abi % MULTI_ARCH] ?: UNKNOWN_ABI_RES
    } else {
      ABI_STRING_RES_MAP[abi] ?: UNKNOWN_ABI_RES
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

  @DrawableRes
  fun getLargeAbiBadgeResource(type: Int): Int {
    return when (type % MULTI_ARCH) {
      ARMV8 -> R.drawable.ic_abi_label_arm64_v8a
      ARMV7 -> R.drawable.ic_abi_label_armeabi_v7a
      ARMV5 -> R.drawable.ic_abi_label_armeabi
      X86_64 -> R.drawable.ic_abi_label_x86_64
      X86 -> R.drawable.ic_abi_label_x86
      MIPS64 -> R.drawable.ic_abi_label_mips64
      MIPS -> R.drawable.ic_abi_label_mips
      RISCV64 -> R.drawable.ic_abi_label_riscv64
      RISCV32 -> R.drawable.ic_abi_label_riscv32
      ERROR -> 0
      else -> 0
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
    return "(${item.size.sizeToString(context, showBytes = false)})"
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
    hasAny: Boolean = false,
    checkCancelled: () -> Unit = {}
  ): List<String> = runCatching {
    checkCancelled()
    ZipFileCompat(sourceFile).use { findDexClasses(it, classes, hasAny, checkCancelled) }
  }.getOrElse {
    if (it is java.util.concurrent.CancellationException) throw it
    emptyList()
  }

  fun findDexClasses(
    zipFile: IZipFile,
    classes: List<String>,
    hasAny: Boolean = false,
    checkCancelled: () -> Unit = {}
  ): List<String> {
    if (classes.isEmpty()) return emptyList()
    val foundClasses = linkedSetOf<String>()
    val distinctClassCount = classes.toSet().size
    return tracePackageUtilsSection(TRACE_FIND_DEX_CLASSES) {
      runCatching {
        zipFile.getZipEntries().asSequence()
          .filter {
            checkCancelled()
            isDexEntryName(it.name)
          }
          .forEach { entry ->
            checkCancelled()
            val remainingClasses = classes.filterNot(foundClasses::contains)
            zipFile.getInputStream(entry).use { inputStream ->
              foundClasses += StreamingDexClassScanner.findClasses(
                inputStream = inputStream,
                classPatterns = remainingClasses,
                hasAny = hasAny,
                entrySize = entry.size.takeIf { it >= 0 },
                checkCancelled = checkCancelled
              )
            }
            if ((hasAny && foundClasses.isNotEmpty()) || foundClasses.size == distinctClassCount) {
              return@runCatching foundClasses.toList()
            }
          }
        foundClasses.toList()
      }.getOrElse {
        if (it is java.util.concurrent.CancellationException) throw it
        emptyList()
      }
    }
  }

  /**
   * Get part of DEX classes (at most 5 DEX's) of an app
   * @param pi PackageInfo of the app
   * @return List of LibStringItem
   */
  fun getDexList(pi: PackageInfo): Collection<LibStringItem> {
    throw RuntimeException("Not implemented")
  }

  internal fun isDexEntryName(name: String): Boolean {
    val len = name.length
    if (len < 11 || !name.startsWith("classes") || !name.endsWith(".dex")) return false
    for (i in 7 until len - 4) {
      val c = name[i]
      if (c < '0' || c > '9') return false
    }
    return true
  }
  private const val KOTLIN_RUNTIME_STRING_THRESHOLD = 2
  private const val MAX_KOTLIN_RUNTIME_SCAN_BYTES = 32 * 1024 * 1024
  private val KOTLIN_RUNTIME_STRING_MARKERS = listOf(
    "kotlin.jvm.functions.Function1",
    "kotlin.coroutines.jvm.internal.BaseContinuationImpl",
    "kotlinx.coroutines.internal.StackTraceRecoveryKt",
    "COROUTINE_SUSPENDED"
  )

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

  fun describeSignature(
    context: Context,
    dateFormat: DateFormat,
    signature: Signature,
    signatureSchemes: List<String>
  ): LibStringItem {
    val bytes = signature.toByteArray()
    val certificate = X509Certificate.getInstance(bytes)
    val serialNumber = "0x${certificate.serialNumber.toString(16)}"
    val source = buildString {
      fun field(label: Int, value: Any?) {
        append(context.getString(label))
        append(":")
        appendLine(value)
      }

      field(R.string.signature_scheme_version, signatureSchemes.joinToString(", "))
      field(R.string.signature_version, certificate.version + 1)
      field(R.string.signature_serial_number, "${certificate.serialNumber}($serialNumber)")
      field(R.string.signature_issuer, certificate.issuerDN)
      field(R.string.signature_subject, certificate.subjectDN)
      field(R.string.signature_validity_not_before, dateFormat.format(certificate.notBefore))
      field(R.string.signature_validity_not_after, dateFormat.format(certificate.notAfter))
      field(R.string.signature_public_key_format, certificate.publicKey.format)
      field(R.string.signature_public_key_algorithm, certificate.publicKey.algorithm)
      when (val key = certificate.publicKey) {
        is RSAPublicKey -> {
          field(R.string.signature_public_key_exponent, "${key.publicExponent}(0x${key.publicExponent.toString(16)})")
          field(R.string.signature_public_key_modulus_size, key.modulus.toString(2).length)
          field(R.string.signature_public_key_modulus, key.modulus.toByteArray().toHexString(":"))
        }

        is DSAPublicKey -> {
          field(R.string.signature_public_key_y, key.y)
        }

        else -> {
          field(R.string.signature_public_key_type, key.javaClass.simpleName)
        }
      }
      field(R.string.signature_algorithm_name, certificate.sigAlgName)
      field(R.string.signature_algorithm_oid, certificate.sigAlgOID)
      field(R.string.signature_md5, bytes.md5(":"))
      field(R.string.signature_sha1, bytes.sha1(":"))
      field(R.string.signature_sha256, bytes.sha256(":"))
      append(context.getString(R.string.signature_char_string))
      append(":")
      append(signature.toCharsString())
    }
    return LibStringItem(serialNumber, 0, source, null)
  }

  fun getBuildVersionsInfo(
    packageInfo: PackageInfo?,
    packageName: String,
    isApexPackage: Boolean = false
  ): CharSequence {
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
      if (OsUtils.atLeastQ() && isApexPackage) {
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
