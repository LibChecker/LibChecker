package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class GlobalAppListSettingsRepository : AppListSettingsRepository {
  override val displayOptions: Int
    get() = GlobalValues.advancedOptions

  override val displayOptionsChanges: Flow<Int> = GlobalValues.preferencesFlow
    .filter { it.first == Constants.PREF_ADVANCED_OPTIONS }
    .map { it.second as Int }

  override suspend fun notifyDisplayOptionsChanged(diff: Int) {
    GlobalValues.preferencesFlow.emit(Constants.PREF_ADVANCED_OPTIONS to diff)
  }
}
