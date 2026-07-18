package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildPredicateStatisticDataUseCaseTest {

  private val nativeLibraries = mutableMapOf<String, Set<String>>()
  private val useCase = BuildPredicateStatisticDataUseCase(
    object : StatisticEvidenceRepository {
      override fun hasNativeLibrary(packageName: String, libraryName: String): Boolean {
        return libraryName in nativeLibraries[packageName].orEmpty()
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
    nativeLibraries["flutter"] = setOf("libflutter.so", "libapp.so")
    nativeLibraries["native"] = setOf("libnative.so")
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
