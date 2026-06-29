package com.absinthe.libchecker.domain.statistics.chart.usecase

import android.content.pm.PackageInfo
import android.os.Trace
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ZipDataOffsetReader
import com.absinthe.libchecker.utils.elf.ElfParser
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.ABI_VALUE_TO_INSTRUCTION_SET_MAP
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_16_KB
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as JavaZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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
        val abi = item.abi.toInt()
        if (PackageUtils.hasNoNativeLibs(abi)) {
          noNativeLibs.add(item)
          return@runCatching
        }

        val packageInfo = traceSection(TRACE_GET_PACKAGE_INFO) {
          installedAppRepository.getPackageInfo(item.packageName)
        } ?: return@runCatching
        if (is16KBAligned(packageInfo, abi)) {
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
    return traceSection(TRACE_ZIP_DATA_OFFSET) {
      ZipDataOffsetReader.read(file, entryNames)
    }
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
  }
}

data class PageSize16KBChartData(
  val support16KB: List<LCItem>,
  val notSupport16KB: List<LCItem>,
  val noNativeLibs: List<LCItem>
)
