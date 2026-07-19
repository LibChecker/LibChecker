package com.absinthe.libchecker.domain.app.detail.statistics

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetsSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringPattern
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeAppStatisticRulesUseCaseTest {

  private val flutterQuery = StatisticArtifactQuery.NativeLibrary("libflutter.so")
  private val securityPasteQuery = StatisticArtifactQuery.DexClasses(
    listOf(
      StatisticDexClassQuery(
        name = StatisticStringPattern(
          operator = StatisticStringOperator.EQUAL,
          value = "Lcom/os/widget/SecurityPasteView;"
        )
      )
    )
  )
  private var selectedStatistics = listOf(
    predicateDefinition("builtin.local", StatisticSource.BUILTIN),
    predicateDefinition("official.flutter", StatisticSource.OFFICIAL),
    facetsDefinition()
  )
  private var evidenceBatchCount = 0
  private var analyzedPackageName: String? = null
  private val useCase = AnalyzeAppStatisticRulesUseCase(
    catalogRepository = object : StatisticCatalogRepository {
      override suspend fun getSelectedStatistics(): List<StatisticDefinition> = selectedStatistics

      override suspend fun getAvailableStatistics(): List<StatisticDefinition> = selectedStatistics

      override suspend fun refreshAvailableStatistics(): List<StatisticDefinition>? = selectedStatistics

      override suspend fun setSelectedStatisticIds(ids: List<String>) = Unit
    },
    evidenceRepository = object : StatisticEvidenceRepository {
      override fun matches(packageName: String, query: StatisticArtifactQuery): Boolean {
        error("Single-app analysis must use the PackageInfo batch API")
      }

      override fun matchesAll(
        packageInfo: PackageInfo,
        queries: Set<StatisticArtifactQuery>,
        onProgress: (Int) -> Unit
      ): Map<StatisticArtifactQuery, Boolean> {
        evidenceBatchCount++
        analyzedPackageName = packageInfo.packageName
        onProgress(50)
        onProgress(100)
        return queries.associateWith { query ->
          query == flutterQuery || query == securityPasteQuery
        }
      }
    }
  )

  @Test
  fun `analyzes selected official rules in chart order with one evidence batch`() = runBlocking {
    val progress = mutableListOf<Int>()

    val result = useCase(packageInfo(), progress::add)

    assertEquals(listOf("official.flutter", "official.itgsa"), result.map { it.definition.id })
    assertTrue(result[0].matched)
    assertEquals(emptyList<String>(), result[0].matchedFacetIds)
    assertTrue(result[1].matched)
    assertEquals(listOf("security-paste-view"), result[1].matchedFacetIds)
    assertEquals(1, evidenceBatchCount)
    assertEquals("com.example.app", analyzedPackageName)
    assertEquals(0, progress.first())
    assertEquals(100, progress.last())
    assertTrue(progress.zipWithNext().all { (previous, next) -> previous <= next })
  }

  @Test
  fun `returns empty without scanning when chart has no selected online rules`() = runBlocking {
    selectedStatistics = listOf(predicateDefinition("builtin.local", StatisticSource.BUILTIN))
    evidenceBatchCount = 0
    val progress = mutableListOf<Int>()

    val result = useCase(packageInfo(), progress::add)

    assertTrue(result.isEmpty())
    assertEquals(0, evidenceBatchCount)
    assertEquals(listOf(0, 100), progress)
  }

  @Test
  fun `reports unmatched when a predicate has no matching evidence`() = runBlocking {
    selectedStatistics = listOf(
      predicateDefinition(
        id = "official.target-sdk",
        source = StatisticSource.OFFICIAL,
        predicate = StatisticPredicateSpec(
          matchedTitle = title("Modern"),
          unmatchedTitle = title("Legacy"),
          evidence = StatisticEvidence.TARGET_SDK,
          operator = StatisticComparisonOperator.GREATER_THAN_OR_EQUAL,
          value = StatisticPredicateValue(integer = 36)
        )
      )
    )

    val result = useCase(packageInfo())

    assertFalse(result.single().matched)
  }

  private fun packageInfo() = PackageInfo().apply {
    packageName = "com.example.app"
    applicationInfo = ApplicationInfo().apply {
      targetSdkVersion = 35
      sourceDir = "/tmp/example.apk"
    }
  }

  private fun predicateDefinition(
    id: String,
    source: StatisticSource,
    predicate: StatisticPredicateSpec = StatisticPredicateSpec(
      matchedTitle = title("Flutter apps"),
      unmatchedTitle = title("Other apps"),
      evidence = StatisticEvidence.NATIVE_LIBRARY,
      operator = StatisticComparisonOperator.CONTAINS,
      value = StatisticPredicateValue(string = flutterQuery.name)
    )
  ) = StatisticDefinition(
    id = id,
    revision = 1,
    source = source,
    title = title(id),
    icon = StatisticIconSpec(asset = "icons/example.svg"),
    calculation = StatisticCalculationSpec(
      kind = StatisticCalculationKind.PREDICATE,
      predicate = predicate
    )
  )

  private fun facetsDefinition() = StatisticDefinition(
    id = "official.itgsa",
    revision = 1,
    source = StatisticSource.OFFICIAL,
    title = title("ITGSA"),
    icon = StatisticIconSpec(asset = "icons/itgsa.svg"),
    calculation = StatisticCalculationSpec(
      kind = StatisticCalculationKind.FACETS,
      facets = StatisticFacetsSpec(
        matchedTitle = title("ITGSA apps"),
        unmatchedTitle = title("Other apps"),
        items = listOf(
          StatisticFacetSpec(
            id = "voip-service-kit",
            title = title("VoIP Service Kit"),
            condition = StatisticConditionSpec(
              evidence = StatisticEvidence.DEX_CLASS,
              operator = StatisticComparisonOperator.CONTAINS_ANY,
              value = StatisticPredicateValue(
                dexClasses = listOf(
                  StatisticDexClassQuery(
                    name = StatisticStringPattern(
                      operator = StatisticStringOperator.STARTS_WITH,
                      value = "Lcom/voip/service/"
                    )
                  )
                )
              )
            )
          ),
          StatisticFacetSpec(
            id = "security-paste-view",
            title = title("Security Paste View"),
            condition = StatisticConditionSpec(
              evidence = StatisticEvidence.DEX_CLASS,
              operator = StatisticComparisonOperator.CONTAINS_ANY,
              value = StatisticPredicateValue(dexClasses = securityPasteQuery.queries)
            )
          )
        )
      )
    )
  )

  private fun title(value: String) = StatisticTitleSpec(
    translations = mapOf("en" to value)
  )
}
