package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class BuildPredicateStatisticDataUseCase(
  private val evidenceRepository: StatisticEvidenceRepository
) {

  suspend operator fun invoke(
    request: Request,
    onProgress: suspend (Int) -> Unit = {}
  ): PredicateStatisticData? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val matched = mutableListOf<LCItem>()
    val unmatched = mutableListOf<LCItem>()
    val coroutineContext = currentCoroutineContext()
    val itemCount = targets.size
    val condition = request.predicate.condition ?: StatisticConditionSpec(
      evidence = request.predicate.evidence,
      operator = request.predicate.operator,
      value = request.predicate.value
    )
    var progress = 0

    targets.forEachIndexed { index, item ->
      if (!coroutineContext.isActive) return null
      if (item.matches(condition)) {
        matched += item
      } else {
        unmatched += item
      }
      if (itemCount > 0) {
        val nextProgress = (index + 1) * LAST_COMPUTATION_PROGRESS / itemCount
        if (nextProgress > progress) {
          progress = nextProgress
          onProgress(progress)
        }
      }
    }
    return PredicateStatisticData(matched, unmatched)
  }

  private fun LCItem.matches(condition: StatisticConditionSpec): Boolean {
    condition.all?.let { return it.all { child -> matches(child) } }
    condition.any?.let { return it.any { child -> matches(child) } }
    condition.not?.let { return !matches(it) }

    val evidence = checkNotNull(condition.evidence)
    val operator = checkNotNull(condition.operator)
    val value = checkNotNull(condition.value)
    return when (evidence) {
      StatisticEvidence.TARGET_SDK -> compare(
        actual = targetApi.toLong(),
        expected = checkNotNull(value.integer),
        operator = operator
      )

      StatisticEvidence.NATIVE_LIBRARY -> evidenceRepository.matches(
        packageName = packageName,
        query = StatisticArtifactQuery.NativeLibrary(checkNotNull(value.string))
      )

      StatisticEvidence.DEX_CLASS -> evidenceRepository.matches(
        packageName = packageName,
        query = StatisticArtifactQuery.DexClasses(checkNotNull(value.dexClasses))
      )

      StatisticEvidence.MANIFEST_RECEIVER_ACTION -> evidenceRepository.matches(
        packageName = packageName,
        query = StatisticArtifactQuery.ManifestReceiverActions(checkNotNull(value.strings))
      )
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

  data class Request(
    val items: List<LCItem>,
    val predicate: StatisticPredicateSpec,
    val showSystemApps: Boolean
  )

  private companion object {
    const val LAST_COMPUTATION_PROGRESS = 99
  }
}

data class PredicateStatisticData(
  val matched: List<LCItem>,
  val unmatched: List<LCItem>
)
