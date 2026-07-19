package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetsSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringPattern
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildFacetStatisticDataUseCaseTest {

  private val artifactMatches = mutableMapOf<String, Set<StatisticArtifactQuery>>()
  private val batchQueries = mutableListOf<Set<StatisticArtifactQuery>>()
  private val useCase = BuildFacetStatisticDataUseCase(
    object : StatisticEvidenceRepository {
      override fun matches(packageName: String, query: StatisticArtifactQuery): Boolean {
        error("Facet evaluation must use the batch evidence API")
      }

      override fun matchesAll(
        packageName: String,
        queries: Set<StatisticArtifactQuery>
      ): Map<StatisticArtifactQuery, Boolean> {
        batchQueries += queries
        return queries.associateWith { query -> query in artifactMatches[packageName].orEmpty() }
      }
    }
  )

  @Test
  fun `keeps every matched facet in rule order and classifies unmatched apps`() = runBlocking {
    artifactMatches["multi"] = setOf(voipQuery, securityPasteQuery)
    artifactMatches["fair"] = setOf(fairMemoryManifestQuery)

    val result = useCase(
      BuildFacetStatisticDataUseCase.Request(
        items = listOf(item("multi"), item("fair"), item("other")),
        facets = facets,
        showSystemApps = true
      )
    )

    assertEquals(listOf("multi", "fair"), result?.matched?.map { it.packageName })
    assertEquals(listOf("other"), result?.unmatched?.map { it.packageName })
    assertEquals(
      listOf("voip-service-kit", "security-paste-view"),
      result?.matchedFacetIds?.get("multi")
    )
    assertEquals(listOf("fair-runtime-memory"), result?.matchedFacetIds?.get("fair"))
  }

  @Test
  fun `batches all artifact evidence once per app and reports determinate progress`() = runBlocking {
    val progress = mutableListOf<Int>()

    useCase(
      BuildFacetStatisticDataUseCase.Request(
        items = listOf(item("one"), item("two"), item("three")),
        facets = facets,
        showSystemApps = true
      ),
      progress::add
    )

    assertEquals(3, batchQueries.size)
    assertEquals(setOf(voipQuery, fairMemoryDexQuery, fairMemoryManifestQuery, securityPasteQuery), batchQueries.first())
    assertEquals(listOf(33, 66, 99), progress)
  }

  private val voipQuery = dexQuery("Lcom/voip/service/", StatisticStringOperator.STARTS_WITH)
  private val fairMemoryDexQuery = dexQuery("Lcom/example/FairMemory;", StatisticStringOperator.EQUAL)
  private val fairMemoryManifestQuery = StatisticArtifactQuery.ManifestReceiverActions(
    listOf("itgsa.intent.action.TRIM", "itgsa.intent.action.KILL")
  )
  private val securityPasteQuery = dexQuery(
    "Lcom/os/widget/SecurityPasteView;",
    StatisticStringOperator.EQUAL
  )

  private val facets = StatisticFacetsSpec(
    matchedTitle = title("Matched"),
    unmatchedTitle = title("Other"),
    items = listOf(
      facet("voip-service-kit", condition(voipQuery)),
      facet(
        "fair-runtime-memory",
        StatisticConditionSpec(
          any = listOf(
            condition(fairMemoryDexQuery),
            StatisticConditionSpec(
              evidence = StatisticEvidence.MANIFEST_RECEIVER_ACTION,
              operator = StatisticComparisonOperator.CONTAINS_ANY,
              value = StatisticPredicateValue(
                strings = fairMemoryManifestQuery.actions
              )
            )
          )
        )
      ),
      facet("security-paste-view", condition(securityPasteQuery))
    )
  )

  private fun facet(id: String, condition: StatisticConditionSpec) = StatisticFacetSpec(
    id = id,
    title = title(id),
    condition = condition
  )

  private fun condition(query: StatisticArtifactQuery.DexClasses) = StatisticConditionSpec(
    evidence = StatisticEvidence.DEX_CLASS,
    operator = StatisticComparisonOperator.CONTAINS_ANY,
    value = StatisticPredicateValue(dexClasses = query.queries)
  )

  private fun dexQuery(
    value: String,
    operator: StatisticStringOperator
  ) = StatisticArtifactQuery.DexClasses(
    listOf(
      StatisticDexClassQuery(
        name = StatisticStringPattern(operator = operator, value = value)
      )
    )
  )

  private fun title(value: String) = StatisticTitleSpec(
    translations = mapOf("en" to value)
  )

  private fun item(packageName: String) = LCItem(
    packageName = packageName,
    label = packageName,
    versionName = "1",
    versionCode = 1,
    installedTime = 0,
    lastUpdatedTime = 0,
    isSystem = false,
    abi = 0,
    features = 0,
    targetApi = 35,
    variant = 0
  )
}
