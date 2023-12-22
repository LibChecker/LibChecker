package com.absinthe.libchecker.features.applist.detail.bean

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class FeatureItem(
  @DrawableRes val res: Int,
  @ColorInt val colorFilterInt: Int? = null,
  val action: () -> Unit
)
