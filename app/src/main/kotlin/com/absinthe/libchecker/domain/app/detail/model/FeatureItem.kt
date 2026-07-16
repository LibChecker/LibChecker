package com.absinthe.libchecker.domain.app.detail.model

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction

data class FeatureItem(
  @StringRes val titleRes: Int,
  val icon: FeatureItemIcon,
  val action: AppDetailFeatureAction
)

sealed interface FeatureItemIcon {
  data class Resource(
    @DrawableRes val res: Int,
    @ColorInt val tint: Int? = null
  ) : FeatureItemIcon

  data class Drawables(
    val values: List<Drawable>
  ) : FeatureItemIcon
}
