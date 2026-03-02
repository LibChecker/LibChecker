package com.absinthe.libchecker.features.applist.detail.bean

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class FeatureItem(
  @DrawableRes val res: Int,
  val drawables: List<Drawable>? = null,
  @ColorInt val colorFilterInt: Int? = null,
  val action: () -> Unit
)
