package com.absinthe.libchecker.domain.app.detail.statistics

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import com.absinthe.libchecker.domain.statistics.chart.usecase.StatisticConditionEvaluator
import com.absinthe.libchecker.domain.statistics.chart.usecase.toConditionSpec
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class AnalyzeAppStatisticRulesUseCase(
  private val catalogRepository: StatisticCatalogRepository,
  private val evidenceRepository: StatisticEvidenceRepository
) {
  private val conditionEvaluator = StatisticConditionEvaluator()

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    onProgress: (Int) -> Unit = {}
  ): List<AppStatisticRuleAnalysis> {
    val coroutineContext = currentCoroutineContext()
    onProgress(0)
    val definitions = getSelectedOfficialDefinitions()
    coroutineContext.ensureActive()
    if (definitions.isEmpty()) {
      onProgress(100)
      return emptyList()
    }

    onProgress(CATALOG_PROGRESS)
    val artifactQueries = definitions
      .flatMap { definition -> definition.conditions() }
      .flatMapTo(LinkedHashSet(), conditionEvaluator::collectArtifactQueries)
    val artifactMatches = evidenceRepository.matchesAll(
      packageInfo = packageInfo,
      queries = artifactQueries,
      onProgress = { evidenceProgress ->
        coroutineContext.ensureActive()
        onProgress(
          CATALOG_PROGRESS +
            evidenceProgress * (EVIDENCE_PROGRESS - CATALOG_PROGRESS) / 100
        )
      }
    )
    coroutineContext.ensureActive()

    val targetApi = packageInfo.applicationInfo?.targetSdkVersion ?: 0
    val analyses = definitions.mapIndexedNotNull { index, definition ->
      coroutineContext.ensureActive()
      val analysis = definition.analyze(targetApi, artifactMatches)
      val progress = EVIDENCE_PROGRESS +
        (index + 1) * (100 - EVIDENCE_PROGRESS) / definitions.size
      onProgress(progress)
      analysis
    }
    onProgress(100)
    val (matched, unmatched) = analyses.partition(AppStatisticRuleAnalysis::matched)
    return matched + unmatched
  }

  suspend fun hasSelectedRules(): Boolean {
    return getSelectedOfficialDefinitions().isNotEmpty()
  }

  private suspend fun getSelectedOfficialDefinitions(): List<StatisticDefinition> {
    return catalogRepository.getSelectedStatistics()
      .filter { definition -> definition.source == StatisticSource.OFFICIAL }
  }

  private fun StatisticDefinition.analyze(
    targetApi: Int,
    artifactMatches: Map<StatisticArtifactQuery, Boolean>
  ): AppStatisticRuleAnalysis? {
    return when (calculation.kind) {
      StatisticCalculationKind.PREDICATE -> {
        val predicate = calculation.predicate ?: return null
        AppStatisticRuleAnalysis(
          definition = this,
          matched = conditionEvaluator.matches(
            targetApi = targetApi,
            condition = predicate.toConditionSpec(),
            artifactMatches = artifactMatches
          )
        )
      }

      StatisticCalculationKind.FACETS -> {
        val facets = calculation.facets ?: return null
        val matchedFacetIds = facets.items.mapNotNull { facet ->
          facet.id.takeIf {
            conditionEvaluator.matches(
              targetApi = targetApi,
              condition = facet.condition,
              artifactMatches = artifactMatches
            )
          }
        }
        AppStatisticRuleAnalysis(
          definition = this,
          matched = matchedFacetIds.isNotEmpty(),
          matchedFacetIds = matchedFacetIds
        )
      }

      StatisticCalculationKind.NATIVE -> null
    }
  }

  private fun StatisticDefinition.conditions(): List<StatisticConditionSpec> {
    return when (calculation.kind) {
      StatisticCalculationKind.PREDICATE -> listOfNotNull(
        calculation.predicate?.toConditionSpec()
      )

      StatisticCalculationKind.FACETS ->
        calculation.facets?.items
          ?.map { facet -> facet.condition }
          .orEmpty()

      StatisticCalculationKind.NATIVE -> emptyList()
    }
  }

  private companion object {
    const val CATALOG_PROGRESS = 5
    const val EVIDENCE_PROGRESS = 90
  }
}

data class AppStatisticRuleAnalysis(
  val definition: StatisticDefinition,
  val matched: Boolean,
  val matchedFacetIds: List<String> = emptyList()
)
