package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFingerprint
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartRenderRequestTest {

  @Test
  fun `feature fingerprint tracks feature initialization changes`() {
    val before = createChartRenderRequest(
      items = listOf(item(features = -1)),
      statistic = statistic(StatisticFingerprint.FEATURES),
      useDetailedAbiChart = false,
      showSystemApps = false
    )
    val after = createChartRenderRequest(
      items = listOf(item(features = 2)),
      statistic = statistic(StatisticFingerprint.FEATURES),
      useDetailedAbiChart = false,
      showSystemApps = false
    )

    assertNotEquals(before.key.itemsHash, after.key.itemsHash)
  }

  @Test
  fun `standard fingerprint ignores feature initialization changes`() {
    val before = createChartRenderRequest(
      items = listOf(item(features = -1)),
      statistic = statistic(StatisticFingerprint.STANDARD),
      useDetailedAbiChart = false,
      showSystemApps = false
    )
    val after = createChartRenderRequest(
      items = listOf(item(features = 2)),
      statistic = statistic(StatisticFingerprint.STANDARD),
      useDetailedAbiChart = false,
      showSystemApps = false
    )

    assertEquals(before.key.itemsHash, after.key.itemsHash)
  }

  @Test
  fun `loading progress continues only within the same chart configuration`() {
    val original = createChartRenderRequest(
      items = listOf(item(features = 0)),
      statistic = statistic(StatisticFingerprint.STANDARD),
      useDetailedAbiChart = false,
      showSystemApps = false
    ).key
    val updatedItems = createChartRenderRequest(
      items = listOf(item(features = 0, versionCode = 2)),
      statistic = statistic(StatisticFingerprint.STANDARD),
      useDetailedAbiChart = false,
      showSystemApps = false
    ).key
    val detailed = original.copy(useDetailedAbiChart = true)
    val includingSystemApps = original.copy(showSystemApps = true)

    assertTrue(original.canContinueLoadingProgress(updatedItems))
    assertFalse(original.canContinueLoadingProgress(detailed))
    assertFalse(original.canContinueLoadingProgress(includingSystemApps))
  }

  private fun statistic(fingerprint: StatisticFingerprint): StatisticDefinition {
    return StatisticDefinition(
      id = "test",
      revision = 1,
      source = StatisticSource.BUILTIN,
      title = StatisticTitleSpec(translations = mapOf("en" to "Test")),
      icon = StatisticIconSpec(drawable = StatisticDrawableIcon.ABI),
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.NATIVE,
        nativeOperator = StatisticNativeOperator.ABI
      ),
      fingerprint = fingerprint
    )
  }

  private fun item(
    features: Int,
    versionCode: Long = 1
  ): LCItem {
    return LCItem(
      packageName = "com.example.app",
      label = "Example",
      versionName = "1.0",
      versionCode = versionCode,
      installedTime = 1,
      lastUpdatedTime = 1,
      isSystem = false,
      abi = 0,
      features = features,
      targetApi = 35,
      variant = 0
    )
  }
}
