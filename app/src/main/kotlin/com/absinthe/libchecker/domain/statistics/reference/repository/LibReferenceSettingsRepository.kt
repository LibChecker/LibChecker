package com.absinthe.libchecker.domain.statistics.reference.repository

import kotlinx.coroutines.flow.Flow

interface LibReferenceSettingsRepository {
  val appListDisplayOptions: Int
  val threshold: Int
  var options: Int
  val showSystemApps: Boolean
  val colorfulRuleIcon: Boolean
  val thresholdChanges: Flow<Int>
  val showSystemAppsChanges: Flow<Unit>
  val colorfulRuleIconChanges: Flow<Boolean>

  suspend fun setThreshold(threshold: Int)
}
