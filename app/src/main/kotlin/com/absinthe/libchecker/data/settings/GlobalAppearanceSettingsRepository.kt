package com.absinthe.libchecker.data.settings

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.settings.AppearanceSettingsRepository

class GlobalAppearanceSettingsRepository : AppearanceSettingsRepository {
  override var darkMode: String
    get() = GlobalValues.darkMode
    set(value) {
      GlobalValues.darkMode = value
    }
}
