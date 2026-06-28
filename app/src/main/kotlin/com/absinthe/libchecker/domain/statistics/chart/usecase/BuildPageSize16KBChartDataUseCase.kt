package com.absinthe.libchecker.domain.statistics.chart.usecase

import android.content.pm.PackageInfo
import android.os.Trace
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.elf.ElfParser
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.ABI_VALUE_TO_INSTRUCTION_SET_MAP
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_16_KB
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as JavaZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import timber.log.Timber

class BuildPageSize16KBChartDataUseCase(
  private val installedAppRepository: InstalledAppRepository
) {
  private val alignmentCache = mutableMapOf<PageSize16KBCacheKey, Boolean>()

  suspend operator fun invoke(
    request: Request,
    onProgress: suspend (Int) -> Unit
  ): PageSize16KBChartData? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val support16KB = mutableListOf<LCItem>()
    val notSupport16KB = mutableListOf<LCItem>()
    val noNativeLibs = mutableListOf<LCItem>()
    val coroutineContext = currentCoroutineContext()
    val itemCount = targets.size
    var progress = 0

    targets.forEachIndexed { index, item ->
      if (!coroutineContext.isActive) {
        return null
      }

      runCatching {
        val packageInfo = traceSection(TRACE_GET_PACKAGE_INFO) {
          installedAppRepository.getPackageInfo(item.packageName)
        } ?: return@runCatching
        if (PackageUtils.hasNoNativeLibs(item.abi.toInt())) {
          noNativeLibs.add(item)
        } else if (is16KBAligned(packageInfo, item.abi.toInt())) {
          support16KB.add(item)
        } else {
          notSupport16KB.add(item)
        }
      }.onFailure {
        Timber.e(it)
      }

      if (itemCount > 0) {
        val nextProgress = index * 100 / itemCount
        if (nextProgress > progress) {
          progress = nextProgress
          onProgress(progress)
        }
      }
    }

    return PageSize16KBChartData(
      support16KB = support16KB,
      notSupport16KB = notSupport16KB,
      noNativeLibs = noNativeLibs
    )
  }

  data class Request(
    val items: List<LCItem>,
    val showSystemApps: Boolean
  )

  private fun is16KBAligned(packageInfo: PackageInfo, abi: Int): Boolean {
    if (GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT.not()) {
      return false
    }
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return false
    val sourceFile = File(sourceDir)
    if (sourceFile.exists().not()) {
      return false
    }

    val cacheKey = PageSize16KBCacheKey(
      packageName = packageInfo.packageName,
      lastUpdateTime = packageInfo.lastUpdateTime,
      abi = abi,
      sourceDir = sourceDir,
      splitSourceDirs = PackageUtils.getSplitsSourceDir(packageInfo)?.toList().orEmpty()
    )
    return alignmentCache.getOrPut(cacheKey) {
      checkSource16KBAlignment(packageInfo, abi, sourceFile)
    }
  }

  private fun checkSource16KBAlignment(
    packageInfo: PackageInfo,
    abi: Int,
    sourceFile: File
  ): Boolean {
    return traceSection(TRACE_CHECK_SOURCE) {
      val abiSplitFiles = getAbiSplitFiles(packageInfo, abi)
      when (
        val baseResult = traceSection(TRACE_CHECK_BASE_SOURCE) {
          checkApk16KBAlignment(
            file = sourceFile,
            abi = abi,
            precheckNoNativeLibs = abiSplitFiles.isNotEmpty()
          )
        }
      ) {
        PageSize16KBScanResult.Compatible -> return@traceSection true
        PageSize16KBScanResult.Incompatible -> return@traceSection false
        PageSize16KBScanResult.NoNativeLibs -> Unit
      }

      var hasSplitNativeLibs = false
      abiSplitFiles.forEach { split ->
        when (traceSection(TRACE_CHECK_SPLIT_SOURCE) { checkApk16KBAlignment(split, abi) }) {
          PageSize16KBScanResult.Compatible -> hasSplitNativeLibs = true
          PageSize16KBScanResult.Incompatible -> return@traceSection false
          PageSize16KBScanResult.NoNativeLibs -> Unit
        }
      }
      if (hasSplitNativeLibs) {
        return@traceSection true
      }

      checkNativeLibraryDir16KBAlignment(packageInfo)
    }
  }

  private fun getAbiSplitFiles(packageInfo: PackageInfo, abi: Int): List<File> {
    val instructionSet = ABI_VALUE_TO_INSTRUCTION_SET_MAP[abi] ?: return emptyList()
    return PackageUtils.getSplitsSourceDir(packageInfo)
      ?.asSequence()
      ?.filter { split -> split.substringAfterLast(File.separator).contains(instructionSet) }
      ?.map(::File)
      ?.toList()
      .orEmpty()
  }

  private fun checkApk16KBAlignment(
    file: File,
    abi: Int,
    precheckNoNativeLibs: Boolean = false
  ): PageSize16KBScanResult {
    if (file.exists().not() || file.canRead().not()) {
      return PageSize16KBScanResult.NoNativeLibs
    }
    val abiString = ABI_STRING_MAP[abi % MULTI_ARCH] ?: return PageSize16KBScanResult.NoNativeLibs
    val sourceDir = "lib$APK_ENTRY_SEPARATOR$abiString"
    if (precheckNoNativeLibs && hasMatchingNativeLibEntry(file, sourceDir) == false) {
      return PageSize16KBScanResult.NoNativeLibs
    }
    return traceSection(TRACE_SCAN_APK) {
      var hasNativeLibs = false

      try {
        traceSection(TRACE_OPEN_ZIP) { JavaZipFile(file) }.use { zipFile ->
          val nativeEntries = traceSection(TRACE_MATCH_ZIP_ENTRIES) {
            zipFile.entries().asSequence()
              .filter { entry ->
                !entry.isDirectory &&
                  entry.name.endsWith(".so") &&
                  entry.name.startsWith(sourceDir)
              }
              .toList()
          }
          val storedEntryNames = nativeEntries
            .asSequence()
            .filter { it.method == ZipEntry.STORED }
            .map { it.name }
            .toSet()
          val storedEntryOffsets by lazy {
            getZipDataOffsets(file, storedEntryNames)
          }
          nativeEntries.forEach { entry ->
            val pageSize = parseElfMinPageSize(zipFile, entry)
            if (pageSize <= 0) {
              return@forEach
            }
            hasNativeLibs = true
            if (pageSize % PAGE_SIZE_16_KB != 0) {
              return@traceSection PageSize16KBScanResult.Incompatible
            }
            val zipAlignment = if (entry.method == ZipEntry.STORED) {
              val dataOffset = storedEntryOffsets[entry.name]
                ?: return@traceSection PageSize16KBScanResult.Incompatible
              getZipAlignment(dataOffset)
            } else {
              -1L
            }
            if (zipAlignment > 0L && zipAlignment < PAGE_SIZE_16_KB) {
              return@traceSection PageSize16KBScanResult.Incompatible
            }
          }
        }
      } catch (e: OutOfMemoryError) {
        Timber.w(e, "Failed to check 16 KB page-size alignment from ${file.absolutePath}")
        return@traceSection PageSize16KBScanResult.Incompatible
      } catch (e: Exception) {
        Timber.w(e, "Failed to check 16 KB page-size alignment from ${file.absolutePath}")
        return@traceSection PageSize16KBScanResult.Incompatible
      }

      if (hasNativeLibs) {
        PageSize16KBScanResult.Compatible
      } else {
        PageSize16KBScanResult.NoNativeLibs
      }
    }
  }

  private fun hasMatchingNativeLibEntry(file: File, sourceDir: String): Boolean? {
    return traceSection(TRACE_PRECHECK_BASE_APK) {
      runCatching {
        JavaZipFile(file).use { zipFile ->
          val entries = zipFile.entries()
          while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (
              !entry.isDirectory &&
              entry.name.endsWith(".so") &&
              entry.name.startsWith(sourceDir)
            ) {
              return@traceSection true
            }
          }
        }
        false
      }.onFailure {
        Timber.w(it, "Failed to precheck 16 KB page-size native entries from ${file.absolutePath}")
      }.getOrNull()
    }
  }

  private fun parseElfMinPageSize(zipFile: JavaZipFile, entry: ZipEntry): Int {
    return traceSection(TRACE_PARSE_ELF) {
      runCatching {
        ElfParser(zipFile.getInputStream(entry)).use { parser ->
          parser.parseHeader()
          parser.getMinPageSize()
        }
      }.getOrDefault(-1)
    }
  }

  private fun getZipDataOffsets(file: File, entryNames: Set<String>): Map<String, Long> {
    if (entryNames.isEmpty()) {
      return emptyMap()
    }
    val offsets = traceSection(TRACE_ZIP_DATA_OFFSET) {
      readZipDataOffsets(file, entryNames)
    }
    if (offsets.keys.containsAll(entryNames)) {
      return offsets
    }
    return readZipDataOffsetsWithApache(file, entryNames)
  }

  private fun readZipDataOffsets(file: File, entryNames: Set<String>): Map<String, Long> {
    return runCatching {
      RandomAccessFile(file, "r").use { randomAccessFile ->
        val eocdOffset = randomAccessFile.findEndOfCentralDirectoryOffset()
        if (eocdOffset < 0L) {
          return@runCatching emptyMap()
        }

        val eocd = ByteArray(ZIP_EOCD_MIN_SIZE)
        randomAccessFile.seek(eocdOffset)
        randomAccessFile.readFully(eocd)
        val centralDirectorySize = eocd.readUInt32Le(ZIP_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET)
        val centralDirectoryOffset = eocd.readUInt32Le(ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET)
        if (
          centralDirectorySize == ZIP_UINT32_MAX ||
          centralDirectoryOffset == ZIP_UINT32_MAX
        ) {
          return@runCatching emptyMap()
        }

        val offsets = mutableMapOf<String, Long>()
        val remainingEntryNames = entryNames.toMutableSet()
        var remainingBytes = centralDirectorySize
        randomAccessFile.seek(centralDirectoryOffset)
        while (remainingBytes >= ZIP_CENTRAL_DIRECTORY_FIXED_SIZE && remainingEntryNames.isNotEmpty()) {
          val entryOffset = randomAccessFile.filePointer
          val header = ByteArray(ZIP_CENTRAL_DIRECTORY_FIXED_SIZE)
          randomAccessFile.readFully(header)
          if (header.readUInt32Le(0) != ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
            return@runCatching offsets
          }

          val nameSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_NAME_SIZE_OFFSET)
          val extraSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_EXTRA_SIZE_OFFSET)
          val commentSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_COMMENT_SIZE_OFFSET)
          val localHeaderOffset = header.readUInt32Le(ZIP_CENTRAL_DIRECTORY_LOCAL_HEADER_OFFSET)
          val nameBytes = ByteArray(nameSize)
          randomAccessFile.readFully(nameBytes)

          val name = nameBytes.toString(Charsets.UTF_8)
          if (name in remainingEntryNames && localHeaderOffset != ZIP_UINT32_MAX) {
            val dataOffset = randomAccessFile.readLocalDataOffset(localHeaderOffset)
            if (dataOffset > 0L) {
              offsets[name] = dataOffset
              remainingEntryNames.remove(name)
            }
          }

          randomAccessFile.seek(randomAccessFile.filePointer + extraSize + commentSize)
          remainingBytes -= randomAccessFile.filePointer - entryOffset
        }
        offsets
      }
    }.onFailure {
      Timber.w(it, "Failed to read ZIP data offsets from ${file.absolutePath}")
    }.getOrDefault(emptyMap())
  }

  private fun readZipDataOffsetsWithApache(file: File, entryNames: Set<String>): Map<String, Long> {
    return runCatching {
      ZipFile.Builder().setFile(file).get().use { zipFile ->
        entryNames.associateWith { entryName ->
          val entry = zipFile.getEntry(entryName)
            ?: throw IllegalArgumentException("ZIP entry $entryName was not found in ${file.absolutePath}")
          getDataOffsetMethod.invoke(zipFile, entry) as Long
        }
      }
    }.onFailure {
      Timber.w(it, "Failed to read ZIP data offsets with Commons Compress from ${file.absolutePath}")
    }.getOrDefault(emptyMap())
  }

  private fun RandomAccessFile.findEndOfCentralDirectoryOffset(): Long {
    val fileSize = length()
    val searchSize = minOf(fileSize, ZIP_EOCD_MAX_SEARCH_SIZE.toLong()).toInt()
    if (searchSize < ZIP_EOCD_MIN_SIZE) {
      return -1L
    }
    val buffer = ByteArray(searchSize)
    seek(fileSize - searchSize)
    readFully(buffer)
    for (offset in searchSize - ZIP_EOCD_MIN_SIZE downTo 0) {
      if (buffer.readUInt32Le(offset) == ZIP_EOCD_SIGNATURE) {
        return fileSize - searchSize + offset
      }
    }
    return -1L
  }

  private fun RandomAccessFile.readLocalDataOffset(localHeaderOffset: Long): Long {
    val header = ByteArray(ZIP_LOCAL_FILE_HEADER_FIXED_SIZE)
    seek(localHeaderOffset)
    readFully(header)
    if (header.readUInt32Le(0) != ZIP_LOCAL_FILE_HEADER_SIGNATURE) {
      return -1L
    }
    val nameSize = header.readUInt16Le(ZIP_LOCAL_FILE_HEADER_NAME_SIZE_OFFSET)
    val extraSize = header.readUInt16Le(ZIP_LOCAL_FILE_HEADER_EXTRA_SIZE_OFFSET)
    return localHeaderOffset + ZIP_LOCAL_FILE_HEADER_FIXED_SIZE + nameSize + extraSize
  }

  private fun ByteArray.readUInt16Le(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
      ((this[offset + 1].toInt() and 0xff) shl 8)
  }

  private fun ByteArray.readUInt32Le(offset: Int): Long {
    return (this[offset].toLong() and 0xff) or
      ((this[offset + 1].toLong() and 0xff) shl 8) or
      ((this[offset + 2].toLong() and 0xff) shl 16) or
      ((this[offset + 3].toLong() and 0xff) shl 24)
  }

  private fun getZipAlignment(offset: Long): Long {
    return traceSection(TRACE_ZIP_ALIGNMENT) {
      if (offset > 0L) {
        java.lang.Long.lowestOneBit(offset)
      } else {
        -1L
      }
    }
  }

  private fun checkNativeLibraryDir16KBAlignment(packageInfo: PackageInfo): Boolean {
    return traceSection(TRACE_NATIVE_DIR) {
      val nativePath = packageInfo.applicationInfo?.nativeLibraryDir ?: return@traceSection false
      val nativeLibs = File(nativePath).listFiles()
        ?.asSequence()
        ?.filter { it.isFile && it.extension == "so" }
        ?.distinctBy { it.name }
        ?: return@traceSection false

      var hasNativeLibs = false
      nativeLibs.forEach { lib ->
        val pageSize = runCatching {
          ElfParser(lib).use { parser ->
            parser.parseHeader()
            parser.getMinPageSize()
          }
        }.getOrDefault(-1)
        if (pageSize <= 0) {
          return@forEach
        }
        hasNativeLibs = true
        if (pageSize % PAGE_SIZE_16_KB != 0) {
          return@traceSection false
        }
      }
      hasNativeLibs
    }
  }

  private inline fun <T> traceSection(sectionName: String, block: () -> T): T {
    Trace.beginSection(sectionName)
    return try {
      block()
    } finally {
      Trace.endSection()
    }
  }

  private data class PageSize16KBCacheKey(
    val packageName: String,
    val lastUpdateTime: Long,
    val abi: Int,
    val sourceDir: String,
    val splitSourceDirs: List<String>
  )

  private enum class PageSize16KBScanResult {
    Compatible,
    Incompatible,
    NoNativeLibs
  }

  private companion object {
    private const val APK_ENTRY_SEPARATOR = '/'
    private const val TRACE_GET_PACKAGE_INFO = "LC 16KB getPackageInfo"
    private const val TRACE_CHECK_SOURCE = "LC 16KB checkSource"
    private const val TRACE_CHECK_BASE_SOURCE = "LC 16KB checkBaseSource"
    private const val TRACE_CHECK_SPLIT_SOURCE = "LC 16KB checkSplitSource"
    private const val TRACE_PRECHECK_BASE_APK = "LC 16KB precheckBaseApk"
    private const val TRACE_SCAN_APK = "LC 16KB scanApk"
    private const val TRACE_OPEN_ZIP = "LC 16KB openZip"
    private const val TRACE_MATCH_ZIP_ENTRIES = "LC 16KB matchZipEntries"
    private const val TRACE_PARSE_ELF = "LC 16KB parseElf"
    private const val TRACE_ZIP_DATA_OFFSET = "LC 16KB zipDataOffset"
    private const val TRACE_ZIP_ALIGNMENT = "LC 16KB zipAlign"
    private const val TRACE_NATIVE_DIR = "LC 16KB nativeDir"
    private const val ZIP_EOCD_SIGNATURE = 0x06054b50L
    private const val ZIP_EOCD_MIN_SIZE = 22
    private const val ZIP_EOCD_MAX_SEARCH_SIZE = ZIP_EOCD_MIN_SIZE + 0xffff
    private const val ZIP_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET = 12
    private const val ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET = 16
    private const val ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50L
    private const val ZIP_CENTRAL_DIRECTORY_FIXED_SIZE = 46
    private const val ZIP_CENTRAL_DIRECTORY_NAME_SIZE_OFFSET = 28
    private const val ZIP_CENTRAL_DIRECTORY_EXTRA_SIZE_OFFSET = 30
    private const val ZIP_CENTRAL_DIRECTORY_COMMENT_SIZE_OFFSET = 32
    private const val ZIP_CENTRAL_DIRECTORY_LOCAL_HEADER_OFFSET = 42
    private const val ZIP_LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50L
    private const val ZIP_LOCAL_FILE_HEADER_FIXED_SIZE = 30
    private const val ZIP_LOCAL_FILE_HEADER_NAME_SIZE_OFFSET = 26
    private const val ZIP_LOCAL_FILE_HEADER_EXTRA_SIZE_OFFSET = 28
    private const val ZIP_UINT32_MAX = 0xffffffffL
    private val getDataOffsetMethod by lazy {
      ZipFile::class.java.getDeclaredMethod("getDataOffset", ZipArchiveEntry::class.java).apply {
        isAccessible = true
      }
    }
  }
}

data class PageSize16KBChartData(
  val support16KB: List<LCItem>,
  val notSupport16KB: List<LCItem>,
  val noNativeLibs: List<LCItem>
)
