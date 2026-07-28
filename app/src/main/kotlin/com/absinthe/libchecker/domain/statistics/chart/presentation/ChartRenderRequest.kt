package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFingerprint

data class ChartRenderRequest(
  val key: ChartRenderRequestKey,
  val items: List<LCItem>,
  val statistic: StatisticDefinition
)

data class ChartRenderRequestKey(
  val statisticKey: String,
  val useDetailedAbiChart: Boolean,
  val showSystemApps: Boolean,
  val itemsHash: Int
) {
  fun canContinueLoadingProgress(other: ChartRenderRequestKey): Boolean {
    return statisticKey == other.statisticKey &&
      useDetailedAbiChart == other.useDetailedAbiChart &&
      showSystemApps == other.showSystemApps
  }
}

fun createChartRenderRequest(
  items: List<LCItem>,
  statistic: StatisticDefinition,
  useDetailedAbiChart: Boolean,
  showSystemApps: Boolean
): ChartRenderRequest {
  return ChartRenderRequest(
    key = ChartRenderRequestKey(
      statisticKey = "${statistic.id}@${statistic.revision}",
      useDetailedAbiChart = useDetailedAbiChart,
      showSystemApps = showSystemApps,
      itemsHash = items.chartRequestHash(statistic.fingerprint)
    ),
    items = items,
    statistic = statistic
  )
}

private fun List<LCItem>.chartRequestHash(fingerprint: StatisticFingerprint): Int {
  return fold(1) { result, item ->
    31 * result + item.chartRequestHash(fingerprint)
  }
}

private fun LCItem.chartRequestHash(fingerprint: StatisticFingerprint): Int {
  return when (fingerprint) {
    StatisticFingerprint.ARTIFACT -> chart16KBRequestHash()
    StatisticFingerprint.FEATURES -> fullChartRequestHash(includeFeatures = true)
    StatisticFingerprint.STANDARD -> fullChartRequestHash(includeFeatures = false)
  }
}

private fun LCItem.chart16KBRequestHash(): Int {
  var result = packageName.hashCode()
  result = 31 * result + isSystem.hashCode()
  result = 31 * result + abi.hashCode()
  result = 31 * result + versionCode.hashCode()
  result = 31 * result + lastUpdatedTime.hashCode()
  return result
}

private fun LCItem.fullChartRequestHash(includeFeatures: Boolean): Int {
  var result = packageName.hashCode()
  result = 31 * result + label.hashCode()
  result = 31 * result + versionName.hashCode()
  result = 31 * result + versionCode.hashCode()
  result = 31 * result + installedTime.hashCode()
  result = 31 * result + lastUpdatedTime.hashCode()
  result = 31 * result + isSystem.hashCode()
  result = 31 * result + abi.hashCode()
  result = 31 * result + if (includeFeatures) features else 0
  result = 31 * result + targetApi.hashCode()
  result = 31 * result + variant.hashCode()
  return result
}
