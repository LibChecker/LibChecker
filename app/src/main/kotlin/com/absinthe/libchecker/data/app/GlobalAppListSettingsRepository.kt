package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.AppListSettingsRepository

class GlobalAppListSettingsRepository : AppListSettingsRepository {
  override val displayOptions: Int
    get() = GlobalValues.advancedOptions
}
