package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository

class GlobalChartSettingsRepository : ChartSettingsRepository {
  override val appListDisplayOptions: Int
    get() = GlobalValues.advancedOptions

  override val showSystemApps: Boolean
    get() = GlobalValues.isShowSystemApps

  override var isDetailedAbiChart: Boolean
    get() = GlobalValues.isDetailedAbiChart
    set(value) {
      GlobalValues.isDetailedAbiChart = value
    }
}
