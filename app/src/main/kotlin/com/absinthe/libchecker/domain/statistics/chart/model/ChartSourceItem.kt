package com.absinthe.libchecker.domain.statistics.chart.model

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.database.entity.LCItem

data class ChartSourceItem(
  @DrawableRes val iconRes: Int,
  val isGrayIcon: Boolean,
  val data: List<LCItem>
)
