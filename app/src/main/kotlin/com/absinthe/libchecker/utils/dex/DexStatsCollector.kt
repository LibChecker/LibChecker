package com.absinthe.libchecker.utils.dex

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.toJson
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class DexEntryInfo(
  val name: String,
  val size: Long,
  val classCount: Int,
  val crc32: Long
)

@JsonClass(generateAdapter = true)
data class ResourceEntryInfo(
  val name: String,
  val size: Long,
  val crc32: Long
)

object DexStatsCollector {

  internal data class DexSource(
    val name: String,
    val path: String
  )

  data class DexStats(
    val entries: List<DexEntryInfo>,
    val resourceEntries: List<ResourceEntryInfo>,
    val isDexComplete: Boolean,
    val isResourceComplete: Boolean
  ) {
    val resourcesSize: Long
      get() = resourceEntries.sumOf(ResourceEntryInfo::size)
  }

  fun collect(applicationInfo: ApplicationInfo): DexStats {
    val sourceDir = applicationInfo.sourceDir ?: return INCOMPLETE_STATS
    return collect(sourceDir, applicationInfo.splitSourceDirs.orEmpty())
  }

  fun collect(packageInfo: PackageInfo): DexStats {
    val applicationInfo = packageInfo.applicationInfo ?: return INCOMPLETE_STATS
    val sourceDir = applicationInfo.sourceDir ?: return INCOMPLETE_STATS
    val splitSourceDirs = PackageUtils.getSplitsSourceDir(packageInfo).orEmpty()
    val declaredSplitNames: Array<out String>? = packageInfo.splitNames
    val splitNames = declaredSplitNames?.takeIf { it.size == splitSourceDirs.size }
    return collect(sourceDir, splitSourceDirs, splitNames)
  }

  private fun collect(
    sourceDir: String,
    splitSourceDirs: Array<out String>,
    splitNames: Array<out String>? = null
  ): DexStats {
    val sources = buildList {
      add(DexSource(BASE_SOURCE_NAME, sourceDir))
      splitSourceDirs.forEachIndexed { index, path ->
        val fileName = splitNames?.getOrNull(index)
          ?.ifBlank { null }
          ?: File(path).nameWithoutExtension
            .ifBlank { (index + 1).toString() }
        add(DexSource("$SPLIT_SOURCE_PREFIX$fileName", path))
      }
    }
    return collect(sources)
  }

  fun collect(sourceDir: String): DexStats {
    return collect(listOf(DexSource(BASE_SOURCE_NAME, sourceDir)))
  }

  internal fun collect(sources: List<DexSource>): DexStats {
    return runCatching {
      require(sources.size <= MAX_SOURCE_COUNT)
      require(sources.distinctBy(DexSource::name).size == sources.size)
      var remainingDexEntries = MAX_DEX_ENTRY_COUNT
      val sourceStats = sources.map { source ->
        readSourceStats(source, remainingDexEntries).also { stats ->
          remainingDexEntries -= stats.dexEntries?.size ?: 0
        }
      }
      val dexEntries = sourceStats.flatMap { it.dexEntries.orEmpty() }
      require(dexEntries.size <= MAX_DEX_ENTRY_COUNT)
      require(dexEntries.sumOf(DexEntry::size) <= MAX_TOTAL_DEX_SIZE)

      var isDexComplete = sourceStats.all { it.dexEntries != null }
      val collectedEntries = sourceStats.flatMap { stats ->
        val sourceDexEntries = stats.dexEntries.orEmpty()
        val entryNames = sourceDexEntries.map(DexEntry::entryName)
        val classCounts = countClassesPerDex(File(stats.source.path), entryNames).orEmpty()
        if (entryNames.isNotEmpty() && classCounts.isEmpty()) {
          isDexComplete = false
        }
        sourceDexEntries.map { entry ->
          DexEntryInfo(
            name = "${stats.source.name}/${entry.entryName}",
            size = entry.size,
            classCount = classCounts[entry.entryName] ?: 0,
            crc32 = entry.crc32
          )
        }
      }.sortedBy(DexEntryInfo::name)
      val resourceEntries = sourceStats
        .flatMap { it.resourceEntries.orEmpty() }
        .sortedBy(ResourceEntryInfo::name)
      var isResourceComplete = sourceStats.all { it.resourceEntries != null }
      isDexComplete = isDexComplete &&
        isValidStoredDexStats(collectedEntries) &&
        collectedEntries.toJson().orEmpty().length <= MAX_STORED_STATS_JSON_LENGTH
      isResourceComplete = isResourceComplete &&
        isValidStoredResourceStats(
          resourceEntries,
          resourceEntries.sumOf(ResourceEntryInfo::size)
        ) &&
        resourceEntries.toJson().orEmpty().length <= MAX_STORED_STATS_JSON_LENGTH

      DexStats(
        entries = if (isDexComplete) collectedEntries else emptyList(),
        resourceEntries = if (isResourceComplete) resourceEntries else emptyList(),
        isDexComplete = isDexComplete,
        isResourceComplete = isResourceComplete
      )
    }.getOrDefault(INCOMPLETE_STATS)
  }

  internal fun isValidStoredDexStats(entries: List<DexEntryInfo>): Boolean {
    if (entries.size > MAX_DEX_ENTRY_COUNT) return false
    if (entries.distinctBy(DexEntryInfo::name).size != entries.size) return false
    if (entries.any { entry ->
        entry.name.length > MAX_ENTRY_NAME_LENGTH ||
          !entry.name.matches(STORED_DEX_ENTRY_REGEX) ||
          entry.size !in 0..MAX_DEX_ENTRY_SIZE ||
          entry.classCount < 0 ||
          entry.crc32 !in 0..MAX_CRC32
      }
    ) {
      return false
    }
    if (entries.sumOf(DexEntryInfo::size) > MAX_TOTAL_DEX_SIZE) return false
    return true
  }

  internal fun isValidStoredResourceStats(
    entries: List<ResourceEntryInfo>,
    resourcesSize: Long
  ): Boolean {
    if (entries.size > MAX_SOURCE_COUNT) return false
    if (entries.distinctBy(ResourceEntryInfo::name).size != entries.size) return false
    if (entries.any { entry ->
        entry.name.length > MAX_ENTRY_NAME_LENGTH ||
          !entry.name.matches(STORED_RESOURCE_ENTRY_REGEX) ||
          entry.size !in 0..MAX_RESOURCES_SIZE ||
          entry.crc32 !in 0..MAX_CRC32
      }
    ) {
      return false
    }
    return resourcesSize in 0..MAX_TOTAL_RESOURCES_SIZE &&
      entries.sumOf(ResourceEntryInfo::size) == resourcesSize
  }

  private fun readSourceStats(
    source: DexSource,
    remainingDexEntries: Int
  ): SourceStats {
    return runCatching {
      ZipFileCompat(File(source.path)).use { zipFile ->
        val dexEntries = runCatching {
          zipFile.getZipEntries()
            .asSequence()
            .filter { entry ->
              entry.name.matches(DEX_ENTRY_REGEX)
            }
            .take(remainingDexEntries + 1)
            .map { entry ->
              DexEntry(
                entryName = entry.name,
                size = entry.size,
                crc32 = entry.crc
              )
            }
            .toList()
            .also { entries ->
              require(entries.size <= remainingDexEntries)
              require(entries.distinctBy(DexEntry::entryName).size == entries.size)
              require(
                entries.all {
                  it.size in 0..MAX_DEX_ENTRY_SIZE && it.crc32 in 0..MAX_CRC32
                }
              )
            }
        }.getOrNull()
        val resourceEntries = runCatching {
          zipFile.getEntry(RESOURCES_ARSC)?.let { entry ->
            require(entry.size in 0..MAX_RESOURCES_SIZE)
            require(entry.crc in 0..MAX_CRC32)
            listOf(
              ResourceEntryInfo(
                name = "${source.name}/$RESOURCES_ARSC",
                size = entry.size,
                crc32 = entry.crc
              )
            )
          }.orEmpty()
        }.getOrNull()
        SourceStats(
          source = source,
          dexEntries = dexEntries,
          resourceEntries = resourceEntries
        )
      }
    }.getOrElse {
      SourceStats(
        source = source,
        dexEntries = null,
        resourceEntries = null
      )
    }
  }

  private fun countClassesPerDex(sourceFile: File, entryNames: List<String>): Map<String, Int>? {
    if (entryNames.isEmpty()) {
      return emptyMap()
    }
    return runCatching {
      val container = ZipDexContainer2(sourceFile, null, MAX_DEX_ENTRY_SIZE)
      entryNames.associateWith { entryName ->
        container.getEntry(entryName)?.dexFile?.classes?.size ?: 0
      }
    }.getOrNull()
  }

  private data class SourceStats(
    val source: DexSource,
    val dexEntries: List<DexEntry>?,
    val resourceEntries: List<ResourceEntryInfo>?
  )

  private data class DexEntry(
    val entryName: String,
    val size: Long,
    val crc32: Long
  )

  private const val BASE_SOURCE_NAME = "base"
  private const val SPLIT_SOURCE_PREFIX = "split:"
  private const val MAX_SOURCE_COUNT = 64
  private const val MAX_DEX_ENTRY_COUNT = 100
  private const val MAX_DEX_ENTRY_SIZE = 128L * 1024 * 1024
  private const val MAX_TOTAL_DEX_SIZE = 512L * 1024 * 1024
  private const val MAX_RESOURCES_SIZE = 512L * 1024 * 1024
  private const val MAX_TOTAL_RESOURCES_SIZE = MAX_SOURCE_COUNT * MAX_RESOURCES_SIZE
  private const val MAX_ENTRY_NAME_LENGTH = 256
  private const val MAX_CRC32 = 0xffffffffL
  internal const val MAX_STORED_STATS_JSON_LENGTH = 64 * 1024
  private val INCOMPLETE_STATS = DexStats(emptyList(), emptyList(), false, false)
  private val DEX_ENTRY_REGEX = Regex("^classes(\\d*)\\.dex$")
  private val STORED_DEX_ENTRY_REGEX = Regex("^(base|split:[^/\\r\\n]+)/classes(\\d*)\\.dex$")
  private val STORED_RESOURCE_ENTRY_REGEX =
    Regex("^(base|split:[^/\\r\\n]+)/resources\\.arsc$")
  private const val RESOURCES_ARSC = "resources.arsc"
}
