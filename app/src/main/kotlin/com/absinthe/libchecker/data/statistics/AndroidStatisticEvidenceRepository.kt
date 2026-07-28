package com.absinthe.libchecker.data.statistics

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.LruCache
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexMethodReference
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticManifestElement
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.dex.FastDexFileFactory
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class AndroidStatisticEvidenceRepository(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) : StatisticEvidenceRepository {
  private val dexMatchCache = LruCache<DexMatchCacheKey, Boolean>(DEX_CACHE_SIZE)
  private val archiveEntryMatchCache =
    LruCache<ArchiveEntryMatchCacheKey, Boolean>(ARCHIVE_ENTRY_CACHE_SIZE)
  private val manifestActionCache = LruCache<ArtifactVersion, Set<String>>(MANIFEST_CACHE_SIZE)
  private val manifestAttributeCache =
    LruCache<ArtifactVersion, Map<String, Any>>(MANIFEST_CACHE_SIZE)

  override fun matches(packageName: String, query: StatisticArtifactQuery): Boolean {
    return matchesAll(packageName, setOf(query))[query] == true
  }

  override fun matchesAll(
    packageName: String,
    queries: Set<StatisticArtifactQuery>
  ): Map<StatisticArtifactQuery, Boolean> {
    val packageInfo = installedAppRepository.getPackageInfo(packageName)
      ?: return queries.associateWith { false }
    return matchesAll(packageInfo, queries)
  }

  override fun matchesAll(
    packageInfo: PackageInfo,
    queries: Set<StatisticArtifactQuery>,
    onProgress: (Int) -> Unit
  ): Map<StatisticArtifactQuery, Boolean> {
    if (queries.isEmpty()) {
      onProgress(100)
      return emptyMap()
    }
    return try {
      onProgress(0)
      val results = LinkedHashMap<StatisticArtifactQuery, Boolean>(queries.size)
      val version = packageInfo.artifactVersion()

      val nativeQueries = queries.filterIsInstance<StatisticArtifactQuery.NativeLibrary>()
      if (nativeQueries.isNotEmpty()) {
        val nativeDirectory = packageInfo.applicationInfo?.nativeLibraryDir?.let(::File)
        val extractedLibraries = nativeDirectory?.listFiles()
          ?.filter { it.isFile && it.extension == "so" }
          .orEmpty()
        val libraryNames = if (extractedLibraries.isNotEmpty()) {
          extractedLibraries.mapTo(mutableSetOf(), File::getName)
        } else {
          PackageUtils.getNativeDirLibs(packageInfo, parseElf = false)
            .mapTo(mutableSetOf()) { it.name }
        }
        nativeQueries.forEach { query -> results[query] = query.name in libraryNames }
      }
      onProgress(NATIVE_PROGRESS)

      val archiveQueries = queries.filterIsInstance<StatisticArtifactQuery.ArchiveEntries>()
      if (archiveQueries.isNotEmpty()) {
        if (version == null) {
          archiveQueries.forEach { query -> results[query] = false }
        } else {
          val requestedNames = archiveQueries.flatMapTo(LinkedHashSet(), StatisticArtifactQuery.ArchiveEntries::names)
          val matches = LinkedHashMap<String, Boolean>(requestedNames.size)
          val pendingNames = LinkedHashSet<String>()
          requestedNames.forEach { name ->
            val cached = archiveEntryMatchCache[ArchiveEntryMatchCacheKey(version, name)]
            if (cached == null) {
              pendingNames += name
            } else {
              matches[name] = cached
            }
          }
          if (pendingNames.isNotEmpty()) {
            val foundNames = readArchiveEntries(version.sourcePaths, pendingNames)
            pendingNames.forEach { name ->
              val matched = name in foundNames
              matches[name] = matched
              archiveEntryMatchCache.put(ArchiveEntryMatchCacheKey(version, name), matched)
            }
          }
          archiveQueries.forEach { query ->
            results[query] = query.names.any { name -> matches[name] == true }
          }
        }
      }
      onProgress(ARCHIVE_ENTRY_PROGRESS)

      val manifestQueries = queries.filterIsInstance<StatisticArtifactQuery.ManifestReceiverActions>()
      if (manifestQueries.isNotEmpty()) {
        val actions = version?.let {
          manifestActionCache[it] ?: readManifestReceiverActions(it.sourcePaths)
            .also { actions -> manifestActionCache.put(it, actions) }
        }.orEmpty()
        manifestQueries.forEach { query ->
          results[query] = query.actions.any(actions::contains)
        }
      }

      val manifestAttributeQueries =
        queries.filterIsInstance<StatisticArtifactQuery.ManifestAttribute>()
      if (manifestAttributeQueries.isNotEmpty()) {
        val properties = version?.let {
          manifestAttributeCache[it] ?: readApplicationManifestProperties(it.sourcePaths)
            .also { properties -> manifestAttributeCache.put(it, properties) }
        }.orEmpty()
        val resources = packageInfo.applicationInfo?.let { applicationInfo ->
          runCatching { packageManager.getResourcesForApplication(applicationInfo) }
            .onFailure { error ->
              Timber.w(error, "Unable to resolve statistic manifest resources for %s", packageInfo.packageName)
            }
            .getOrNull()
        }
        manifestAttributeQueries.forEach { query ->
          val attribute = query.query
          val rawValue = if (attribute.element == StatisticManifestElement.APPLICATION) {
            properties[attribute.name.substringAfter(':')]
          } else {
            null
          }
          val actual = resolveManifestBooleanValue(rawValue) { resourceId ->
            runCatching { resources?.getBoolean(resourceId) }.getOrNull()
          }
          results[query] = actual != null && actual == attribute.boolean
        }
      }
      onProgress(MANIFEST_PROGRESS)

      val dexQueries = queries.filterIsInstance<StatisticArtifactQuery.DexClasses>()
      if (dexQueries.isNotEmpty()) {
        if (version == null) {
          dexQueries.forEach { query -> results[query] = false }
        } else {
          val pending = mutableListOf<StatisticArtifactQuery.DexClasses>()
          dexQueries.forEach { query ->
            val cached = dexMatchCache[DexMatchCacheKey(version, query.queries)]
            if (cached == null) {
              pending += query
            } else {
              results[query] = cached
            }
          }
          if (pending.isNotEmpty()) {
            matchesDexClassGroups(
              sourceFiles = version.sourcePaths.map(::File),
              queryGroups = pending,
              onProgress = { dexProgress ->
                onProgress(
                  MANIFEST_PROGRESS +
                    dexProgress * (DEX_PROGRESS - MANIFEST_PROGRESS) / 100
                )
              }
            ).forEach { (query, matched) ->
              results[query] = matched
              dexMatchCache.put(DexMatchCacheKey(version, query.queries), matched)
            }
          }
        }
      }
      onProgress(DEX_PROGRESS)

      queries.associateWith { query -> results[query] == true }.also {
        onProgress(100)
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Throwable) {
      Timber.e(error)
      onProgress(100)
      queries.associateWith { false }
    }
  }

  private fun matchesDexClassGroups(
    sourceFiles: List<File>,
    queryGroups: List<StatisticArtifactQuery.DexClasses>,
    onProgress: (Int) -> Unit
  ): Map<StatisticArtifactQuery.DexClasses, Boolean> {
    if (queryGroups.isEmpty()) return emptyMap()
    val results = queryGroups.associateWithTo(LinkedHashMap()) { false }
    val unmatchedGroups = queryGroups.toMutableSet()
    val readableSourceFiles = sourceFiles.filter(File::isFile)
    if (readableSourceFiles.isEmpty()) {
      onProgress(100)
      return results
    }
    var lastProgress = 0

    fun reportProgress(
      fileIndex: Int,
      entryIndex: Int,
      entryCount: Int,
      classIndex: Int,
      classCount: Int
    ) {
      val fileProgress = (
        entryIndex.toDouble() +
          (classIndex + 1).toDouble() / classCount.coerceAtLeast(1)
        ) / entryCount.coerceAtLeast(1)
      val progress = (
        (fileIndex.toDouble() + fileProgress) * 100 / readableSourceFiles.size
        ).toInt().coerceIn(0, 100)
      if (progress > lastProgress) {
        lastProgress = progress
        onProgress(progress)
      }
    }

    readableSourceFiles.forEachIndexed sourceLoop@{ fileIndex, sourceFile ->
      if (unmatchedGroups.isEmpty()) {
        onProgress(100)
        return results
      }
      val container = try {
        FastDexFileFactory.loadDexContainer(sourceFile, Opcodes.getDefault())
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        Timber.w(error, "Unable to scan statistic DEX evidence from %s", sourceFile)
        reportProgress(fileIndex, 0, 1, 0, 1)
        return@sourceLoop
      }
      val entryNames = container.dexEntryNames.toList()
      if (entryNames.isEmpty()) {
        reportProgress(fileIndex, 0, 1, 0, 1)
        return@sourceLoop
      }
      entryNames.forEachIndexed entryLoop@{ entryIndex, entryName ->
        if (unmatchedGroups.isEmpty()) {
          onProgress(100)
          return results
        }
        val dexFile = container.getEntry(entryName)?.dexFile
        if (dexFile == null) {
          reportProgress(fileIndex, entryIndex, entryNames.size, 0, 1)
          return@entryLoop
        }
        val eligibleQueries = unmatchedGroups.associateWith { group ->
          group.queries.filter { query ->
            query.stringConstants.isEmpty() ||
              dexFile.stringSection.any(query.stringConstants::contains)
          }
        }.filterValues { it.isNotEmpty() }
        if (eligibleQueries.isEmpty()) {
          reportProgress(fileIndex, entryIndex, entryNames.size, 0, 1)
          return@entryLoop
        }
        val matchedGroups = mutableSetOf<StatisticArtifactQuery.DexClasses>()
        val classCount = dexFile.classes.size.coerceAtLeast(1)
        for ((classIndex, classDef) in dexFile.classes.withIndex()) {
          eligibleQueries.forEach { (group, queries) ->
            if (group !in matchedGroups && queries.any { query -> classDef.matches(query) }) {
              matchedGroups += group
            }
          }
          if (classIndex % DEX_PROGRESS_CLASS_INTERVAL == 0 || classIndex == classCount - 1) {
            reportProgress(fileIndex, entryIndex, entryNames.size, classIndex, classCount)
          }
          if (matchedGroups.size == eligibleQueries.size) break
        }
        reportProgress(fileIndex, entryIndex, entryNames.size, classCount - 1, classCount)
        matchedGroups.forEach { group ->
          results[group] = true
          unmatchedGroups -= group
        }
      }
    }
    onProgress(100)
    return results
  }

  private fun ClassDef.matches(query: StatisticDexClassQuery): Boolean {
    val nameMatches = query.name?.let { pattern ->
      when (pattern.operator) {
        StatisticStringOperator.EQUAL -> type == pattern.value
        StatisticStringOperator.STARTS_WITH -> type.startsWith(pattern.value)
      }
    } ?: true
    if (!nameMatches) return false
    if (query.stringConstants.isEmpty() && query.methodReferences.isEmpty()) return true

    var stringMatches = query.stringConstants.isEmpty()
    var methodMatches = query.methodReferences.isEmpty()
    methods.forEach { method ->
      method.implementation?.instructions?.forEach { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.let {
          runCatching { it.reference }.getOrNull()
        } ?: return@forEach
        when (reference) {
          is StringReference -> {
            if (reference.string in query.stringConstants) stringMatches = true
          }

          is MethodReference -> {
            if (query.methodReferences.any { expected -> reference.matches(expected) }) methodMatches = true
          }
        }
        if (stringMatches && methodMatches) return true
      }
    }
    return false
  }

  private fun MethodReference.matches(expected: StatisticDexMethodReference): Boolean {
    return definingClass == expected.definingClass &&
      name == expected.name &&
      (expected.parameterTypes == null || parameterTypes.map(CharSequence::toString) == expected.parameterTypes)
  }

  private fun readManifestReceiverActions(sourcePaths: List<String>): Set<String> {
    return sourcePaths.asSequence()
      .flatMap { sourcePath ->
        runCatching {
          IntentFilterUtils.parseComponentsFromApk(sourcePath)
        }.onFailure { error ->
          Timber.w(error, "Unable to scan statistic manifest evidence from %s", sourcePath)
        }.getOrDefault(emptyList()).asSequence()
      }
      .filter { it.type == RECEIVER }
      .flatMap { it.intentFilters.asSequence() }
      .flatMap { it.actions.asSequence() }
      .toSet()
  }

  private fun readApplicationManifestProperties(sourcePaths: List<String>): Map<String, Any> {
    val baseApk = sourcePaths.firstOrNull()?.let(::File)?.takeIf(File::isFile)
      ?: return emptyMap()
    return runCatching {
      ApplicationReader.getManifestProperties(baseApk).mapValues { it.value }
    }.onFailure { error ->
      Timber.w(error, "Unable to scan statistic application manifest attributes from %s", baseApk)
    }.getOrDefault(emptyMap())
  }

  private fun readArchiveEntries(
    sourcePaths: List<String>,
    requestedNames: Set<String>
  ): Set<String> {
    val foundNames = mutableSetOf<String>()
    val pendingNames = requestedNames.toMutableSet()
    sourcePaths.asSequence()
      .map(::File)
      .filter(File::isFile)
      .forEach { sourceFile ->
        if (pendingNames.isEmpty()) return foundNames
        runCatching {
          ZipFileCompat(sourceFile).use { archive ->
            pendingNames.toList().forEach { name ->
              if (archive.getEntry(name) != null) {
                foundNames += name
                pendingNames -= name
              }
            }
          }
        }.onFailure { error ->
          Timber.w(error, "Unable to scan statistic archive evidence from %s", sourceFile)
        }
      }
    return foundNames
  }

  private fun PackageInfo.artifactVersion(): ArtifactVersion? {
    val appInfo = applicationInfo ?: return null
    val sourcePaths = buildList {
      appInfo.sourceDir?.let(::add)
      appInfo.splitSourceDirs?.let(::addAll)
    }.distinct()
    if (sourcePaths.isEmpty()) return null
    val versionToken = sourcePaths.fold(lastUpdateTime) { token, sourcePath ->
      val sourceFile = File(sourcePath)
      31 * token + sourceFile.lastModified() + sourceFile.length()
    }
    return ArtifactVersion(sourcePaths, versionToken)
  }

  private data class ArtifactVersion(
    val sourcePaths: List<String>,
    val versionToken: Long
  )

  private data class DexMatchCacheKey(
    val version: ArtifactVersion,
    val queries: List<StatisticDexClassQuery>
  )

  private data class ArchiveEntryMatchCacheKey(
    val version: ArtifactVersion,
    val name: String
  )

  private companion object {
    const val DEX_CACHE_SIZE = 512
    const val ARCHIVE_ENTRY_CACHE_SIZE = 512
    const val MANIFEST_CACHE_SIZE = 128
    const val NATIVE_PROGRESS = 10
    const val ARCHIVE_ENTRY_PROGRESS = 20
    const val MANIFEST_PROGRESS = 30
    const val DEX_PROGRESS = 95
    const val DEX_PROGRESS_CLASS_INTERVAL = 64
  }
}

internal fun resolveManifestBooleanValue(
  rawValue: Any?,
  resolveResource: (Int) -> Boolean?
): Boolean? {
  return when (rawValue) {
    is Boolean -> rawValue

    is Number -> resolveResource(rawValue.toInt())

    is String -> when (rawValue.lowercase()) {
      "true", "1" -> true
      "false", "0" -> false
      else -> null
    }

    else -> null
  }
}
