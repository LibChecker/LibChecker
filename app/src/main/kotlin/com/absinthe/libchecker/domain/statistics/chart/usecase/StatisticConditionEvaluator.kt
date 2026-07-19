package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery

internal class StatisticConditionEvaluator {

  fun collectArtifactQueries(condition: StatisticConditionSpec): Set<StatisticArtifactQuery> {
    val queries = LinkedHashSet<StatisticArtifactQuery>()
    condition.collectArtifactQueries(queries)
    return queries
  }

  fun matches(
    item: LCItem,
    condition: StatisticConditionSpec,
    artifactMatches: Map<StatisticArtifactQuery, Boolean>
  ): Boolean {
    condition.all?.let { return it.all { child -> matches(item, child, artifactMatches) } }
    condition.any?.let { return it.any { child -> matches(item, child, artifactMatches) } }
    condition.not?.let { return !matches(item, it, artifactMatches) }

    val evidence = checkNotNull(condition.evidence)
    val operator = checkNotNull(condition.operator)
    val value = checkNotNull(condition.value)
    return when (evidence) {
      StatisticEvidence.TARGET_SDK -> compare(
        actual = item.targetApi.toLong(),
        expected = checkNotNull(value.integer),
        operator = operator
      )

      StatisticEvidence.NATIVE_LIBRARY -> artifactMatches[
        StatisticArtifactQuery.NativeLibrary(checkNotNull(value.string))
      ] == true

      StatisticEvidence.DEX_CLASS -> artifactMatches[
        StatisticArtifactQuery.DexClasses(checkNotNull(value.dexClasses))
      ] == true

      StatisticEvidence.MANIFEST_RECEIVER_ACTION -> artifactMatches[
        StatisticArtifactQuery.ManifestReceiverActions(checkNotNull(value.strings))
      ] == true
    }
  }

  private fun StatisticConditionSpec.collectArtifactQueries(
    destination: MutableSet<StatisticArtifactQuery>
  ) {
    all?.forEach { it.collectArtifactQueries(destination) }
    any?.forEach { it.collectArtifactQueries(destination) }
    not?.collectArtifactQueries(destination)
    val value = value ?: return
    when (evidence) {
      StatisticEvidence.TARGET_SDK, null -> Unit

      StatisticEvidence.NATIVE_LIBRARY -> {
        destination += StatisticArtifactQuery.NativeLibrary(checkNotNull(value.string))
      }

      StatisticEvidence.DEX_CLASS -> {
        destination += StatisticArtifactQuery.DexClasses(checkNotNull(value.dexClasses))
      }

      StatisticEvidence.MANIFEST_RECEIVER_ACTION -> {
        destination += StatisticArtifactQuery.ManifestReceiverActions(checkNotNull(value.strings))
      }
    }
  }

  private fun compare(
    actual: Long,
    expected: Long,
    operator: StatisticComparisonOperator
  ): Boolean {
    return when (operator) {
      StatisticComparisonOperator.EQUAL -> actual == expected
      StatisticComparisonOperator.GREATER_THAN_OR_EQUAL -> actual >= expected
      StatisticComparisonOperator.LESS_THAN_OR_EQUAL -> actual <= expected
      StatisticComparisonOperator.CONTAINS -> false
      StatisticComparisonOperator.CONTAINS_ANY -> false
    }
  }
}
