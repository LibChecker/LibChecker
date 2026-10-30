package com.absinthe.libchecker.utils.elf

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfoHidden
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import android.os.Trace
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV5_STRING
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV7_STRING
import com.absinthe.libchecker.constant.Constants.ARMV7_STRING_UNDERLINE
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.ARMV8_STRING
import com.absinthe.libchecker.constant.Constants.ARMV8_STRING_UNDERLINE
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MIPS
import com.absinthe.libchecker.constant.Constants.MIPS64
import com.absinthe.libchecker.constant.Constants.MIPS64_STRING
import com.absinthe.libchecker.constant.Constants.MIPS_STRING
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.Constants.RISCV32
import com.absinthe.libchecker.constant.Constants.RISCV64
import com.absinthe.libchecker.constant.Constants.RISCV64_STRING
import com.absinthe.libchecker.constant.Constants.RISCV_STRING
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.constant.Constants.X86_64_STRING
import com.absinthe.libchecker.constant.Constants.X86_STRING
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.getSplitsSourceDir
import com.absinthe.libchecker.utils.apk.ZipDataOffsetReader
import com.absinthe.libchecker.utils.elf.ElfInfo
import com.absinthe.libchecker.utils.elf.ElfParser
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.extensions.isOverlay
import com.absinthe.libchecker.utils.manifest.ManifestReader
import com.absinthe.libraries.utils.manager.TimeRecorder
import dev.rikka.tools.refine.Refine
import java.io.File
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import okio.source
import timber.log.Timber

object NativeLibraryAnalyzer {
  private data class NativeZipEntries(
    val entries: List<ZipEntry>,
    val storedEntryNames: Set<String>
  )

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

  private const val ENABLE_GET_APK_FILE_LIBS_LOG = true

  private const val APK_ENTRY_SEPARATOR = '/'

  private const val TRACE_APK_LIBS_DATA_OFFSET = "LC PackageUtils apkLibsDataOffset"

  private const val TRACE_APK_LIBS_MATCH_ENTRIES = "LC PackageUtils apkLibsMatchEntries"

  private const val TRACE_APK_LIBS_OPEN_ZIP = "LC PackageUtils apkLibsOpenZip"

  private const val TRACE_APK_LIBS_PARSE_ELF = "LC PackageUtils apkLibsParseElf"

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
      Timber.w(it, "Failed to scan ABI set from ${file.absolutePath}")
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

  fun isAbi64Bit(abi: Int): Boolean {
    if (abi == NO_LIBS) {
      return Process.is64Bit()
    }
    return abi in ABI_64_BIT
  }

  fun hasNoNativeLibs(abi: Int): Boolean {
    return abi == OVERLAY || (abi % MULTI_ARCH) == NO_LIBS
  }

  /**
   * Check if an app is using 32-bit ABI
   * @return True if is using 32-bit ABI
   */
  fun ApplicationInfo.isUse32BitAbi(): Boolean {
    return runCatching {
      val demands = ManifestReader.getManifestProperties(
        File(sourceDir),
        arrayOf("use32bitAbi")
      )
      demands["use32bitAbi"] as? Boolean == true
    }.getOrElse { false }
  }

  val INSTRUCTION_SET_MAP_TO_ABI_VALUE = mapOf(
    "arm64" to ARMV8,
    "arm" to ARMV7,
    "x86_64" to X86_64,
    "x86" to X86,
    "mips64" to MIPS64,
    "mips" to MIPS,
    "riscv64" to RISCV64,
    "riscv32" to RISCV32
  )

  val ABI_VALUE_TO_INSTRUCTION_SET_MAP = INSTRUCTION_SET_MAP_TO_ABI_VALUE.entries.associate { (k, v) -> v to k }

  val ABI_64_BIT = setOf(ARMV8, X86_64, MIPS64, RISCV64)

  val ABI_32_BIT = setOf(ARMV5, ARMV7, X86, MIPS, RISCV32)

  val STRING_ABI_MAP = mapOf(
    ARMV8_STRING_UNDERLINE to ARMV8,
    ARMV8_STRING to ARMV8,
    ARMV7_STRING_UNDERLINE to ARMV7,
    ARMV7_STRING to ARMV7,
    ARMV5_STRING to ARMV5,
    X86_64_STRING to X86_64,
    X86_STRING to X86,
    MIPS64_STRING to MIPS64,
    MIPS_STRING to MIPS,
    RISCV64_STRING to RISCV64,
    RISCV_STRING to RISCV32
  )

  val ABI_STRING_MAP = STRING_ABI_MAP.entries.associate { (k, v) -> v to k }

  const val PAGE_SIZE_16_KB = 0x4000

  /**
   *
   * An app is considered to be 16KB-aligned only if:
   * - There's at least one native library present
   * - All native libraries have page sizes that are multiples of 16 KB
   * - None of the native libraries are uncompressed and not 16 KB-aligned
   * @see <a href="https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:sdk-common/src/main/java/com/android/ide/common/pagealign/PageAlignUtils.kt">Ref</a>
   *
   */
  fun PackageInfo.is16KBAligned(libs: List<LibStringItem>? = null, isApk: Boolean = false): Boolean {
    if (GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT.not()) return false
    val sourceDir = applicationInfo?.sourceDir ?: return false

    val file = File(sourceDir)
    if (file.exists().not()) {
      return false
    }

    val nativeLibs = (
      libs ?: run {
        val abi = NativeLibraryAnalyzer.getAbi(this, isApk = isApk)
        val abiString = ABI_STRING_MAP[abi % MULTI_ARCH]
        NativeLibraryAnalyzer.getSourceLibs(
          packageInfo = this,
          specifiedAbi = abi,
          parseElf = true
        )[abiString] ?: emptyList()
      }
      ).filter { it.elfInfo.pageSize > 0 }

    return nativeLibs.isNotEmpty() &&
      nativeLibs.all {
        it.elfInfo.pageSize % PAGE_SIZE_16_KB == 0 &&
          (it.elfInfo.zipAlignment <= 0L || it.elfInfo.zipAlignment >= PAGE_SIZE_16_KB)
      }
  }

  /**
   * Check if an app is 16 kb backcompat
   * @return True if is 16 kb backcompat
   * https://source.android.com/docs/core/architecture/16kb-page-size/16kb-backcompat-option
   */
  fun PackageInfo.isPageSizeCompat(openedZip: IZipFile? = null): Boolean {
    return runCatching {
      val demands = if (openedZip != null) {
        val entry = openedZip.getEntry("AndroidManifest.xml") ?: return@runCatching false
        val bytes = openedZip.getInputStream(entry).use(ManifestReader::getBytesFromInputStream)
          ?: return@runCatching false
        ManifestReader.getManifestProperties(bytes, arrayOf("pageSizeCompat"))
      } else {
        ManifestReader.getManifestProperties(
          File(applicationInfo!!.sourceDir),
          arrayOf("pageSizeCompat")
        )
      }
      demands["pageSizeCompat"] as? String == "enabled"
    }.getOrElse { false }
  }
}
