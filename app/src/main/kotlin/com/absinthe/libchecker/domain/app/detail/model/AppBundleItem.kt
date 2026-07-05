package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes

data class AppBundleItem(
  @DrawableRes val iconRes: Int,
  val nameText: String,
  val sizeText: String,
  val contentDescription: String,
  val size: Long
)

fun buildAppBundleItemDescription(vararg parts: CharSequence?): String {
  return buildDetailItemDescription(*parts)
}
