package com.absinthe.libchecker.domain.statistics

interface ChartSettingsRepository {
  val appListDisplayOptions: Int
  val showSystemApps: Boolean
  var isDetailedAbiChart: Boolean
}
