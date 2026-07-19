package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticManifestAttributeQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticManifestElement
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringPattern
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildPredicateStatisticDataUseCaseTest {

  private val artifactMatches = mutableMapOf<String, MutableSet<StatisticArtifactQuery>>()
  private val useCase = BuildPredicateStatisticDataUseCase(
    object : StatisticEvidenceRepository {
      override fun matches(packageName: String, query: StatisticArtifactQuery): Boolean {
        return query in artifactMatches[packageName].orEmpty()
      }
    }
  )
  private val predicate = StatisticPredicateSpec(
    evidence = StatisticEvidence.TARGET_SDK,
    operator = StatisticComparisonOperator.GREATER_THAN_OR_EQUAL,
    value = StatisticPredicateValue(integer = 35),
    matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Modern")),
    unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Legacy"))
  )

  @Test
  fun `classifies target SDK evidence and filters system apps`() = runBlocking {
    val result = useCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = listOf(
          item("modern", targetApi = 35),
          item("legacy", targetApi = 34),
          item("system", targetApi = 35, isSystem = true)
        ),
        predicate = predicate,
        showSystemApps = false
      )
    )

    assertEquals(listOf("modern"), result?.matched?.map { it.packageName })
    assertEquals(listOf("legacy"), result?.unmatched?.map { it.packageName })
  }

  @Test
  fun `includes system apps when requested`() = runBlocking {
    val result = useCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = listOf(item("system", targetApi = 35, isSystem = true)),
        predicate = predicate,
        showSystemApps = true
      )
    )

    assertEquals(listOf("system"), result?.matched?.map { it.packageName })
  }

  @Test
  fun `classifies native library evidence`() = runBlocking {
    artifactMatches["flutter"] = mutableSetOf(
      StatisticArtifactQuery.NativeLibrary("libflutter.so"),
      StatisticArtifactQuery.NativeLibrary("libapp.so")
    )
    artifactMatches["native"] = mutableSetOf(StatisticArtifactQuery.NativeLibrary("libnative.so"))
    val result = useCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = listOf(item("flutter", targetApi = 35), item("native", targetApi = 35)),
        predicate = StatisticPredicateSpec(
          evidence = StatisticEvidence.NATIVE_LIBRARY,
          operator = StatisticComparisonOperator.CONTAINS,
          value = StatisticPredicateValue(string = "libflutter.so"),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Flutter")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other"))
        ),
        showSystemApps = true
      )
    )

    assertEquals(listOf("flutter"), result?.matched?.map { it.packageName })
    assertEquals(listOf("native"), result?.unmatched?.map { it.packageName })
  }

  @Test
  fun `combines generic artifact evidence from a remote condition`() = runBlocking {
    val dexQueries = listOf(
      StatisticDexClassQuery(
        name = StatisticStringPattern(
          operator = StatisticStringOperator.STARTS_WITH,
          value = "Lcom/example/"
        )
      )
    )
    artifactMatches["dex"] = mutableSetOf(StatisticArtifactQuery.DexClasses(dexQueries))
    artifactMatches["manifest"] = mutableSetOf(
      StatisticArtifactQuery.ManifestReceiverActions(listOf("com.example.ACTION"))
    )
    val result = useCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = listOf(
          item("dex", targetApi = 35),
          item("manifest", targetApi = 35),
          item("other", targetApi = 35)
        ),
        predicate = StatisticPredicateSpec(
          condition = StatisticConditionSpec(
            any = listOf(
              StatisticConditionSpec(
                evidence = StatisticEvidence.DEX_CLASS,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(dexClasses = dexQueries)
              ),
              StatisticConditionSpec(
                evidence = StatisticEvidence.MANIFEST_RECEIVER_ACTION,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(strings = listOf("com.example.ACTION"))
              )
            )
          ),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Matched")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other"))
        ),
        showSystemApps = true
      )
    )

    assertEquals(listOf("dex", "manifest"), result?.matched?.map { it.packageName })
    assertEquals(listOf("other"), result?.unmatched?.map { it.packageName })
  }

  @Test
  fun `classifies an application manifest Boolean attribute`() = runBlocking {
    val manifestAttribute = StatisticManifestAttributeQuery(
      element = StatisticManifestElement.APPLICATION,
      name = "android:enableOnBackInvokedCallback",
      boolean = true
    )
    artifactMatches["enabled"] = mutableSetOf(
      StatisticArtifactQuery.ManifestAttribute(manifestAttribute)
    )
    val result = useCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = listOf(item("enabled", targetApi = 33), item("disabled", targetApi = 36)),
        predicate = StatisticPredicateSpec(
          evidence = StatisticEvidence.MANIFEST_ATTRIBUTE,
          operator = StatisticComparisonOperator.EQUAL,
          value = StatisticPredicateValue(manifestAttribute = manifestAttribute),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Enabled")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Not enabled"))
        ),
        showSystemApps = true
      )
    )

    assertEquals(listOf("enabled"), result?.matched?.map { it.packageName })
    assertEquals(listOf("disabled"), result?.unmatched?.map { it.packageName })
  }

  @Test
  fun `reports determinate progress while evaluating predicate evidence`() = runBlocking {
    val progress = mutableListOf<Int>()

    useCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = listOf(
          item("one", targetApi = 35),
          item("two", targetApi = 34),
          item("three", targetApi = 35)
        ),
        predicate = predicate,
        showSystemApps = true
      ),
      progress::add
    )

    assertEquals(listOf(33, 66, 99), progress)
  }

  private fun item(
    packageName: String,
    targetApi: Int,
    isSystem: Boolean = false
  ) = LCItem(
    packageName = packageName,
    label = packageName,
    versionName = "1",
    versionCode = 1,
    installedTime = 0,
    lastUpdatedTime = 0,
    isSystem = isSystem,
    abi = 0,
    features = 0,
    targetApi = targetApi.toShort(),
    variant = 0
  )
}
