package com.absinthe.libchecker.domain.app

import kotlinx.coroutines.flow.Flow

interface AppListSettingsRepository {
  val displayOptions: Int
  val displayOptionsChanges: Flow<Int>

  suspend fun notifyDisplayOptionsChanged(diff: Int)
}
