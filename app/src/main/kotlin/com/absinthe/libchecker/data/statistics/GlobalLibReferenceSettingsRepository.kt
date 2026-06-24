package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository

class GlobalLibReferenceSettingsRepository : LibReferenceSettingsRepository {
  override val appListDisplayOptions: Int
    get() = GlobalValues.advancedOptions

  override val threshold: Int
    get() = GlobalValues.libReferenceThreshold

  override var options: Int
    get() = GlobalValues.libReferenceOptions
    set(value) {
      GlobalValues.libReferenceOptions = value
    }

  override val showSystemApps: Boolean
    get() = GlobalValues.isShowSystemApps
}
