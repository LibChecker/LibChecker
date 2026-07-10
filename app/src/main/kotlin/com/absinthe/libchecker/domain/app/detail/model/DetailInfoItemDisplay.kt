package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DetailInfoItemDisplay(
  @DrawableRes val iconRes: Int,
  @StringRes val tipRes: Int,
  val textStyle: DetailInfoTextStyle,
  val text: CharSequence,
  val linkUrl: String? = null
)

enum class DetailInfoTextStyle {
  TITLE,
  BODY
}
