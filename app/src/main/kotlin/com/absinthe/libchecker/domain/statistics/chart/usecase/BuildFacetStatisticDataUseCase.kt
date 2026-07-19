package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetsSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class BuildFacetStatisticDataUseCase(
  private val evidenceRepository: StatisticEvidenceRepository
) {
  private val conditionEvaluator = StatisticConditionEvaluator()

  suspend operator fun invoke(
    request: Request,
    onProgress: suspend (Int) -> Unit = {}
  ): FacetStatisticData? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val matched = mutableListOf<LCItem>()
    val unmatched = mutableListOf<LCItem>()
    val matchedFacetIds = LinkedHashMap<String, List<String>>()
    val artifactQueries = request.facets.items
      .flatMapTo(LinkedHashSet()) { facet ->
        conditionEvaluator.collectArtifactQueries(facet.condition)
      }
    val coroutineContext = currentCoroutineContext()
    val itemCount = targets.size
    var progress = 0

    targets.forEachIndexed { index, item ->
      if (!coroutineContext.isActive) return null
      val artifactMatches = evidenceRepository.matchesAll(item.packageName, artifactQueries)
      val facetIds = request.facets.items.mapNotNull { facet ->
        facet.id.takeIf {
          conditionEvaluator.matches(item, facet.condition, artifactMatches)
        }
      }
      if (facetIds.isEmpty()) {
        unmatched += item
      } else {
        matched += item
        matchedFacetIds[item.packageName] = facetIds
      }
      if (itemCount > 0) {
        val nextProgress = (index + 1) * LAST_COMPUTATION_PROGRESS / itemCount
        if (nextProgress > progress) {
          progress = nextProgress
          onProgress(progress)
        }
      }
    }
    return FacetStatisticData(
      matched = matched,
      unmatched = unmatched,
      matchedFacetIds = matchedFacetIds
    )
  }

  data class Request(
    val items: List<LCItem>,
    val facets: StatisticFacetsSpec,
    val showSystemApps: Boolean
  )

  private companion object {
    const val LAST_COMPUTATION_PROGRESS = 99
  }
}

data class FacetStatisticData(
  val matched: List<LCItem>,
  val unmatched: List<LCItem>,
  val matchedFacetIds: Map<String, List<String>>
)
