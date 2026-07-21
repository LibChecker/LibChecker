package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartUiStatePlannerTest {
  private val planner = ChartUiStatePlanner()

  @Test
  fun `selection remains stable while definition is visible`() {
    val definitions = listOf(statistic("abi"), statistic("kotlin"))

    val plan = planner.planStatistics(
      definitions = definitions,
      currentStatisticId = "kotlin",
      featureChartsAvailable = true
    )

    assertEquals("kotlin", plan.selectedStatistic?.id)
  }

  @Test
  fun `hidden feature statistic falls back to first visible definition`() {
    val definitions = listOf(
      statistic("abi"),
      statistic("kotlin", requiresFeatureInitialization = true)
    )

    val plan = planner.planStatistics(
      definitions = definitions,
      currentStatisticId = "kotlin",
      featureChartsAvailable = false
    )

    assertEquals(listOf("abi"), plan.visibleStatistics.map(StatisticDefinition::id))
    assertEquals("abi", plan.selectedStatistic?.id)
  }

  private fun statistic(
    id: String,
    requiresFeatureInitialization: Boolean = false
  ): StatisticDefinition {
    return StatisticDefinition(
      id = id,
      revision = 1,
      source = StatisticSource.BUILTIN,
      title = StatisticTitleSpec(translations = mapOf("en" to id)),
      icon = StatisticIconSpec(drawable = StatisticDrawableIcon.ABI),
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.NATIVE,
        nativeOperator = StatisticNativeOperator.ABI
      ),
      requiresFeatureInitialization = requiresFeatureInitialization
    )
  }
}
