package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository

class GlobalLibReferenceSettingsRepository : LibReferenceSettingsRepository {
  override val appListDisplayOptions: Int
    get() = GlobalValues.advancedOptions

  override val threshold: Int
    get() = GlobalValues.libReferenceThreshold

  override val options: Int
    get() = GlobalValues.libReferenceOptions

  override val showSystemApps: Boolean
    get() = GlobalValues.isShowSystemApps
}
