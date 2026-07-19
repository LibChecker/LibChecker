package com.absinthe.libchecker.domain.statistics.chart.repository

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery

interface StatisticEvidenceRepository {

  fun matches(packageName: String, query: StatisticArtifactQuery): Boolean

  fun matchesAll(
    packageName: String,
    queries: Set<StatisticArtifactQuery>
  ): Map<StatisticArtifactQuery, Boolean> {
    return queries.associateWith { query -> matches(packageName, query) }
  }

  fun matchesAll(
    packageInfo: PackageInfo,
    queries: Set<StatisticArtifactQuery>,
    onProgress: (Int) -> Unit = {}
  ): Map<StatisticArtifactQuery, Boolean> {
    onProgress(0)
    return matchesAll(packageInfo.packageName, queries).also {
      onProgress(100)
    }
  }
}

sealed interface StatisticArtifactQuery {

  data class NativeLibrary(val name: String) : StatisticArtifactQuery

  data class DexClasses(val queries: List<StatisticDexClassQuery>) : StatisticArtifactQuery

  data class ArchiveEntries(val names: List<String>) : StatisticArtifactQuery

  data class ManifestReceiverActions(val actions: List<String>) : StatisticArtifactQuery
}
