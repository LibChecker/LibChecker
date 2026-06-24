package com.absinthe.libchecker.domain.app

import kotlinx.coroutines.flow.Flow

interface AppListSettingsRepository {
  val displayOptions: Int
  val itemDisplayOptions: Int
  val colorfulRuleIcon: Boolean
  val displayOptionsChanges: Flow<Int>

  fun setDisplayOptions(options: Int)
  fun setItemDisplayOptions(options: Int)

  suspend fun notifyDisplayOptionsChanged(diff: Int)
}
