package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

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

  override val thresholdChanges: Flow<Int> = GlobalValues.preferencesFlow
    .filter { it.first == Constants.PREF_LIB_REF_THRESHOLD }
    .map { it.second as Int }

  override suspend fun setThreshold(threshold: Int) {
    GlobalValues.libReferenceThreshold = threshold
    GlobalValues.preferencesFlow.emit(Constants.PREF_LIB_REF_THRESHOLD to threshold)
  }
}
