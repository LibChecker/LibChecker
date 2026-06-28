package com.absinthe.libchecker.domain.statistics.chart.usecase

import android.content.pm.PackageInfo
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
import java.util.zip.ZipEntry
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
        val packageInfo = installedAppRepository.getPackageInfo(item.packageName) ?: return@runCatching
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
    when (val baseResult = checkApk16KBAlignment(sourceFile, abi)) {
      PageSize16KBScanResult.Compatible -> return true
      PageSize16KBScanResult.Incompatible -> return false
      PageSize16KBScanResult.NoNativeLibs -> Unit
    }

    var hasSplitNativeLibs = false
    getAbiSplitFiles(packageInfo, abi).forEach { split ->
      when (checkApk16KBAlignment(split, abi)) {
        PageSize16KBScanResult.Compatible -> hasSplitNativeLibs = true
        PageSize16KBScanResult.Incompatible -> return false
        PageSize16KBScanResult.NoNativeLibs -> Unit
      }
    }
    if (hasSplitNativeLibs) {
      return true
    }

    return checkNativeLibraryDir16KBAlignment(packageInfo)
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

  private fun checkApk16KBAlignment(file: File, abi: Int): PageSize16KBScanResult {
    if (file.exists().not() || file.canRead().not()) {
      return PageSize16KBScanResult.NoNativeLibs
    }
    val abiString = ABI_STRING_MAP[abi % MULTI_ARCH] ?: return PageSize16KBScanResult.NoNativeLibs
    val sourceDir = "lib$APK_ENTRY_SEPARATOR$abiString"
    var hasNativeLibs = false

    try {
      ZipFile.Builder().setFile(file).get().use { zipFile ->
        zipFile.entries.asSequence()
          .filter { entry ->
            !entry.isDirectory &&
              entry.name.endsWith(".so") &&
              entry.name.startsWith(sourceDir)
          }
          .forEach { entry ->
            val pageSize = parseElfMinPageSize(zipFile, entry)
            if (pageSize <= 0) {
              return@forEach
            }
            hasNativeLibs = true
            if (pageSize % PAGE_SIZE_16_KB != 0) {
              return PageSize16KBScanResult.Incompatible
            }
            val zipAlignment = if (entry.method == ZipEntry.STORED) {
              getZipAlignment(zipFile, entry)
            } else {
              -1L
            }
            if (zipAlignment > 0L && zipAlignment < PAGE_SIZE_16_KB) {
              return PageSize16KBScanResult.Incompatible
            }
          }
      }
    } catch (e: OutOfMemoryError) {
      Timber.w(e, "Failed to check 16 KB page-size alignment from ${file.absolutePath}")
      return PageSize16KBScanResult.Incompatible
    } catch (e: Exception) {
      Timber.w(e, "Failed to check 16 KB page-size alignment from ${file.absolutePath}")
      return PageSize16KBScanResult.Incompatible
    }

    return if (hasNativeLibs) {
      PageSize16KBScanResult.Compatible
    } else {
      PageSize16KBScanResult.NoNativeLibs
    }
  }

  private fun parseElfMinPageSize(zipFile: ZipFile, entry: ZipArchiveEntry): Int {
    return runCatching {
      ElfParser(zipFile.getInputStream(entry)).use { parser ->
        parser.parseHeader()
        parser.getMinPageSize()
      }
    }.getOrDefault(-1)
  }

  private fun getZipAlignment(zipFile: ZipFile, entry: ZipArchiveEntry): Long {
    val offset = getDataOffsetMethod.invoke(zipFile, entry) as Long
    return if (offset > 0L) {
      java.lang.Long.lowestOneBit(offset)
    } else {
      -1L
    }
  }

  private fun checkNativeLibraryDir16KBAlignment(packageInfo: PackageInfo): Boolean {
    val nativePath = packageInfo.applicationInfo?.nativeLibraryDir ?: return false
    val nativeLibs = File(nativePath).listFiles()
      ?.asSequence()
      ?.filter { it.isFile && it.extension == "so" }
      ?.distinctBy { it.name }
      ?: return false

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
        return false
      }
    }
    return hasNativeLibs
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
