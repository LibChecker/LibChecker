package com.absinthe.libchecker.domain.settings.usecase

import androidx.appcompat.app.AppCompatDelegate
import com.absinthe.libchecker.constant.Constants

object NightModeResolver {
  fun resolve(darkMode: String): Int {
    return when (darkMode) {
      Constants.DARK_MODE_OFF -> AppCompatDelegate.MODE_NIGHT_NO
      Constants.DARK_MODE_ON -> AppCompatDelegate.MODE_NIGHT_YES
      Constants.DARK_MODE_FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
  }
}
