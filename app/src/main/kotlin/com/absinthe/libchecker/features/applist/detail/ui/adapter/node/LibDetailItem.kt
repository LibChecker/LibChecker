package com.absinthe.libchecker.features.applist.detail.ui.adapter.node

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes

data class LibDetailItem(
  @DrawableRes val iconRes: Int,
  @StringRes val tipRes: Int,
  @StyleRes val textStyleRes: Int,
  val text: String
)
