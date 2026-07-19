package com.absinthe.libchecker.data.statistics

import android.util.LruCache
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexMethodReference
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.dex.FastDexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import timber.log.Timber

class AndroidStatisticEvidenceRepository(
  private val installedAppRepository: InstalledAppRepository
) : StatisticEvidenceRepository {
  private val dexMatchCache = LruCache<DexMatchCacheKey, Boolean>(DEX_CACHE_SIZE)
  private val manifestActionCache = LruCache<ArtifactVersion, Set<String>>(MANIFEST_CACHE_SIZE)

  override fun matches(packageName: String, query: StatisticArtifactQuery): Boolean {
    return matchesAll(packageName, setOf(query))[query] == true
  }

  override fun matchesAll(
    packageName: String,
    queries: Set<StatisticArtifactQuery>
  ): Map<StatisticArtifactQuery, Boolean> {
    if (queries.isEmpty()) return emptyMap()
    return runCatching {
      val packageInfo = installedAppRepository.getPackageInfo(packageName)
        ?: return@runCatching queries.associateWith { false }
      val results = LinkedHashMap<StatisticArtifactQuery, Boolean>(queries.size)
      val version = packageInfo.applicationInfo?.sourceDir?.let {
        ArtifactVersion(it, packageInfo.lastUpdateTime)
      }

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

      val manifestQueries = queries.filterIsInstance<StatisticArtifactQuery.ManifestReceiverActions>()
      if (manifestQueries.isNotEmpty()) {
        val actions = version?.let {
          manifestActionCache[it] ?: readManifestReceiverActions(it.sourceDir)
            .also { actions -> manifestActionCache.put(it, actions) }
        }.orEmpty()
        manifestQueries.forEach { query ->
          results[query] = query.actions.any(actions::contains)
        }
      }

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
            matchesDexClassGroups(File(version.sourceDir), pending).forEach { (query, matched) ->
              results[query] = matched
              dexMatchCache.put(DexMatchCacheKey(version, query.queries), matched)
            }
          }
        }
      }

      queries.associateWith { query -> results[query] == true }
    }.onFailure {
      Timber.e(it)
    }.getOrElse {
      queries.associateWith { false }
    }
  }

  private fun matchesDexClassGroups(
    sourceFile: File,
    queryGroups: List<StatisticArtifactQuery.DexClasses>
  ): Map<StatisticArtifactQuery.DexClasses, Boolean> {
    if (queryGroups.isEmpty()) return emptyMap()
    val results = queryGroups.associateWithTo(LinkedHashMap()) { false }
    val unmatchedGroups = queryGroups.toMutableSet()
    val container = FastDexFileFactory.loadDexContainer(sourceFile, Opcodes.getDefault())
    for (entryName in container.dexEntryNames) {
      if (unmatchedGroups.isEmpty()) break
      val dexFile = container.getEntry(entryName)?.dexFile ?: continue
      val eligibleQueries = unmatchedGroups.associateWith { group ->
        group.queries.filter { query ->
          query.stringConstants.isEmpty() ||
            dexFile.stringSection.any(query.stringConstants::contains)
        }
      }.filterValues { it.isNotEmpty() }
      if (eligibleQueries.isEmpty()) continue
      val matchedGroups = mutableSetOf<StatisticArtifactQuery.DexClasses>()
      for (classDef in dexFile.classes) {
        eligibleQueries.forEach { (group, queries) ->
          if (group !in matchedGroups && queries.any { query -> classDef.matches(query) }) {
            matchedGroups += group
          }
        }
        if (matchedGroups.size == eligibleQueries.size) break
      }
      matchedGroups.forEach { group ->
        results[group] = true
        unmatchedGroups -= group
      }
    }
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

  private fun readManifestReceiverActions(sourceDir: String): Set<String> {
    return IntentFilterUtils.parseComponentsFromApk(sourceDir)
      .asSequence()
      .filter { it.type == RECEIVER }
      .flatMap { it.intentFilters.asSequence() }
      .flatMap { it.actions.asSequence() }
      .toSet()
  }

  private data class ArtifactVersion(
    val sourceDir: String,
    val lastUpdateTime: Long
  )

  private data class DexMatchCacheKey(
    val version: ArtifactVersion,
    val queries: List<StatisticDexClassQuery>
  )

  private companion object {
    const val DEX_CACHE_SIZE = 512
    const val MANIFEST_CACHE_SIZE = 128
  }
}
