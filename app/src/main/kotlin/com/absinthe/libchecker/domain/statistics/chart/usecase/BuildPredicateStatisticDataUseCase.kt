package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class BuildPredicateStatisticDataUseCase(
  private val evidenceRepository: StatisticEvidenceRepository
) {
  private val conditionEvaluator = StatisticConditionEvaluator()

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
    val condition = request.predicate.toConditionSpec()
    val artifactQueries = conditionEvaluator.collectArtifactQueries(condition)
    var progress = 0

    targets.forEachIndexed { index, item ->
      if (!coroutineContext.isActive) return null
      val artifactMatches = evidenceRepository.matchesAll(item.packageName, artifactQueries)
      if (conditionEvaluator.matches(item, condition, artifactMatches)) {
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
