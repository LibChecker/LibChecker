package com.absinthe.libchecker.features.applist.detail.ui.adapter.node

import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes

data class XposedDetailItem(
  @DrawableRes val iconRes: Int,
  val tip: String,
  val text: String,
  @StyleRes val textStyleRes: Int
)
