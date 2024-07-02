package com.absinthe.libchecker.features.chart

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.database.entity.LCItem

data class ChartSourceItem(
  @DrawableRes val iconRes: Int,
  val isGrayIcon: Boolean,
  val data: List<LCItem>
)
