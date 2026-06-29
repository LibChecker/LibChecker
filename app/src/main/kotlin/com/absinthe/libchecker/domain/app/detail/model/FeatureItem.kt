package com.absinthe.libchecker.domain.app.detail.model

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class FeatureItem(
  @DrawableRes val res: Int,
  @StringRes val titleRes: Int = 0,
  val drawables: List<Drawable>? = null,
  @ColorInt val colorFilterInt: Int? = null,
  val action: () -> Unit
)
