package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository

class GlobalAppDetailSettingsRepository : AppDetailSettingsRepository {
  override val sortMode: Int
    get() = GlobalValues.libSortMode

  override val processMode: Boolean
    get() = GlobalValues.processMode

  override fun setSortMode(sortMode: Int) {
    GlobalValues.libSortMode = sortMode
  }

  override fun setProcessMode(enabled: Boolean) {
    GlobalValues.processMode = enabled
  }
}
