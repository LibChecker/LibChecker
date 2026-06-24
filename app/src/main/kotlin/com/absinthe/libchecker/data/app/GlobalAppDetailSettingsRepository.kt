package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository

class GlobalAppDetailSettingsRepository : AppDetailSettingsRepository {
  override val sortMode: Int
    get() = GlobalValues.libSortMode
}
