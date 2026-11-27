package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery

internal class StatisticConditionEvaluator {

  /** Resolve cheap facts first, then batch only evidence still needed by unresolved branches. */
  fun evaluateStaged(
    item: LCItem,
    conditions: List<StatisticConditionSpec>,
    resolve: (Set<StatisticArtifactQuery>) -> Map<StatisticArtifactQuery, Boolean>
  ): List<Boolean> {
    val known = LinkedHashMap<StatisticArtifactQuery, Boolean>()
    while (true) {
      val pending = LinkedHashSet<StatisticArtifactQuery>()
      val results = conditions.map { partial(item, it, known, pending) }
      if (results.all { it != null }) return results.map { checkNotNull(it) }
      val stage = pending.minOf(::cost)
      val queries = pending.filterTo(LinkedHashSet()) { cost(it) == stage }
      val resolved = resolve(queries)
      queries.forEach { known[it] = resolved[it] == true }
    }
  }

  private fun partial(
    item: LCItem,
    condition: StatisticConditionSpec,
    known: Map<StatisticArtifactQuery, Boolean>,
    pending: MutableSet<StatisticArtifactQuery>
  ): Boolean? {
    val children = condition.all ?: condition.any
    if (children != null) {
      val childPending = LinkedHashSet<StatisticArtifactQuery>()
      val isAll = condition.all != null
      var unknown = false
      for (child in children) {
        val result = partial(item, child, known, childPending)
        if (result == !isAll) return !isAll
        if (result == null) unknown = true
      }
      if (!unknown) return isAll
      pending += childPending
      return null
    }
    condition.not?.let { return partial(item, it, known, pending)?.not() }
    if (condition.evidence == StatisticEvidence.TARGET_SDK) {
      return matches(item, condition, known)
    }
    val query = condition.artifactQuery()
    return known[query] ?: run {
      pending += query
      null
    }
  }

  private fun cost(query: StatisticArtifactQuery): Int = when (query) {
    is StatisticArtifactQuery.RequestedPermissions, is StatisticArtifactQuery.ApplicationIconTraits -> 0
    is StatisticArtifactQuery.NativeLibrary, is StatisticArtifactQuery.ArchiveEntries -> 0
    is StatisticArtifactQuery.ManifestReceiverActions, is StatisticArtifactQuery.ManifestAttribute -> 1
    is StatisticArtifactQuery.DexClasses -> 2
  }

  fun collectArtifactQueries(condition: StatisticConditionSpec): Set<StatisticArtifactQuery> {
    val queries = LinkedHashSet<StatisticArtifactQuery>()
    condition.collectArtifactQueries(queries)
    return queries
  }

  fun matches(
    item: LCItem,
    condition: StatisticConditionSpec,
    artifactMatches: Map<StatisticArtifactQuery, Boolean>
  ): Boolean = matches(item.targetApi.toInt(), condition, artifactMatches)

  fun matches(
    targetApi: Int,
    condition: StatisticConditionSpec,
    artifactMatches: Map<StatisticArtifactQuery, Boolean>
  ): Boolean {
    condition.all?.let { return it.all { child -> matches(targetApi, child, artifactMatches) } }
    condition.any?.let { return it.any { child -> matches(targetApi, child, artifactMatches) } }
    condition.not?.let { return !matches(targetApi, it, artifactMatches) }

    val evidence = checkNotNull(condition.evidence)
    val operator = checkNotNull(condition.operator)
    val value = checkNotNull(condition.value)
    return when (evidence) {
      StatisticEvidence.TARGET_SDK -> compare(
        actual = targetApi.toLong(),
        expected = checkNotNull(value.integer),
        operator = operator
      )

      else -> artifactMatches[condition.artifactQuery()] == true
    }
  }

  private fun StatisticConditionSpec.collectArtifactQueries(
    destination: MutableSet<StatisticArtifactQuery>
  ) {
    all?.forEach { it.collectArtifactQueries(destination) }
    any?.forEach { it.collectArtifactQueries(destination) }
    not?.collectArtifactQueries(destination)
    if (evidence != null && evidence != StatisticEvidence.TARGET_SDK) destination += artifactQuery()
  }

  private fun StatisticConditionSpec.artifactQuery(): StatisticArtifactQuery {
    val operand = checkNotNull(value)
    return when (checkNotNull(evidence)) {
      StatisticEvidence.NATIVE_LIBRARY -> StatisticArtifactQuery.NativeLibrary(checkNotNull(operand.string))
      StatisticEvidence.DEX_CLASS -> StatisticArtifactQuery.DexClasses(checkNotNull(operand.dexClasses))
      StatisticEvidence.ARCHIVE_ENTRY -> StatisticArtifactQuery.ArchiveEntries(checkNotNull(operand.strings))
      StatisticEvidence.REQUESTED_PERMISSION -> StatisticArtifactQuery.RequestedPermissions(checkNotNull(operand.strings))
      StatisticEvidence.APPLICATION_ICON_TRAIT -> StatisticArtifactQuery.ApplicationIconTraits(checkNotNull(operand.strings))
      StatisticEvidence.MANIFEST_RECEIVER_ACTION -> StatisticArtifactQuery.ManifestReceiverActions(checkNotNull(operand.strings))
      StatisticEvidence.MANIFEST_ATTRIBUTE -> StatisticArtifactQuery.ManifestAttribute(checkNotNull(operand.manifestAttribute))
      StatisticEvidence.TARGET_SDK -> error("Target SDK is available directly on the app item")
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

internal fun StatisticPredicateSpec.toConditionSpec(): StatisticConditionSpec {
  return condition ?: StatisticConditionSpec(
    evidence = evidence,
    operator = operator,
    value = value
  )
}
