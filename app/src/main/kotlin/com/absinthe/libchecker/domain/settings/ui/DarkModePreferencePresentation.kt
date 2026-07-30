package com.absinthe.libchecker.domain.settings.ui

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants

@DrawableRes
internal fun darkModePreferenceIconRes(value: String?): Int {
  return when (value) {
    Constants.DARK_MODE_OFF -> R.drawable.ic_theme_light
    Constants.DARK_MODE_ON -> R.drawable.ic_theme_dark
    else -> R.drawable.ic_theme_system
  }
}
