package com.absinthe.libchecker.data.settings

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.settings.DeveloperSettingsRepository

class GlobalDeveloperSettingsRepository : DeveloperSettingsRepository {
  override var debugMode: Boolean
    get() = GlobalValues.debugMode
    set(value) {
      GlobalValues.debugMode = value
    }
}
