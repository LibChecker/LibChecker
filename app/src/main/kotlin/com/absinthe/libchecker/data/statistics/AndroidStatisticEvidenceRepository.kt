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
    return runCatching {
      val packageInfo = installedAppRepository.getPackageInfo(packageName)
        ?: return@runCatching false
      when (query) {
        is StatisticArtifactQuery.NativeLibrary -> {
          val nativeDirectory = packageInfo.applicationInfo?.nativeLibraryDir?.let(::File)
          val extractedLibraries = nativeDirectory?.listFiles()
            ?.filter { it.isFile && it.extension == "so" }
            .orEmpty()
          if (extractedLibraries.isNotEmpty()) {
            extractedLibraries.any { it.name == query.name }
          } else {
            PackageUtils.getNativeDirLibs(packageInfo, parseElf = false)
              .any { it.name == query.name }
          }
        }

        is StatisticArtifactQuery.DexClasses -> {
          val version = packageInfo.applicationInfo?.sourceDir?.let {
            ArtifactVersion(it, packageInfo.lastUpdateTime)
          } ?: return@runCatching false
          val cacheKey = DexMatchCacheKey(version, query.queries)
          dexMatchCache[cacheKey] ?: matchesDexClasses(File(version.sourceDir), query.queries)
            .also { dexMatchCache.put(cacheKey, it) }
        }

        is StatisticArtifactQuery.ManifestReceiverActions -> {
          val version = packageInfo.applicationInfo?.sourceDir?.let {
            ArtifactVersion(it, packageInfo.lastUpdateTime)
          } ?: return@runCatching false
          val actions = manifestActionCache[version] ?: readManifestReceiverActions(version.sourceDir)
            .also { manifestActionCache.put(version, it) }
          query.actions.any(actions::contains)
        }
      }
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(false)
  }

  private fun matchesDexClasses(
    sourceFile: File,
    queries: List<StatisticDexClassQuery>
  ): Boolean {
    if (queries.isEmpty()) return false
    val container = FastDexFileFactory.loadDexContainer(sourceFile, Opcodes.getDefault())
    container.dexEntryNames.forEach { entryName ->
      val dexFile = container.getEntry(entryName)?.dexFile ?: return@forEach
      val eligibleQueries = queries.filter { query ->
        query.stringConstants.isEmpty() ||
          dexFile.stringSection.any(query.stringConstants::contains)
      }
      if (eligibleQueries.isEmpty()) return@forEach
      dexFile.classes.forEach { classDef ->
        if (eligibleQueries.any { query -> classDef.matches(query) }) return true
      }
    }
    return false
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
