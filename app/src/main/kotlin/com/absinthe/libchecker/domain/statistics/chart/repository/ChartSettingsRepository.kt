package com.absinthe.libchecker.domain.statistics.chart.repository

interface ChartSettingsRepository {
  val appListDisplayOptions: Int
  val showSystemApps: Boolean
  var isDetailedAbiChart: Boolean
}
