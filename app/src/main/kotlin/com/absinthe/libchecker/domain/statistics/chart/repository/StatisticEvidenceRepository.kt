package com.absinthe.libchecker.domain.statistics.chart.repository

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery

interface StatisticEvidenceRepository {

  fun matches(packageName: String, query: StatisticArtifactQuery): Boolean

  fun matchesAll(
    packageName: String,
    queries: Set<StatisticArtifactQuery>
  ): Map<StatisticArtifactQuery, Boolean> {
    return queries.associateWith { query -> matches(packageName, query) }
  }
}

sealed interface StatisticArtifactQuery {

  data class NativeLibrary(val name: String) : StatisticArtifactQuery

  data class DexClasses(val queries: List<StatisticDexClassQuery>) : StatisticArtifactQuery

  data class ManifestReceiverActions(val actions: List<String>) : StatisticArtifactQuery
}
