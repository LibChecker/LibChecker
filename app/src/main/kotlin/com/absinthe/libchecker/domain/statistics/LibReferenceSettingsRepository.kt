package com.absinthe.libchecker.domain.statistics

import kotlinx.coroutines.flow.Flow

interface LibReferenceSettingsRepository {
  val appListDisplayOptions: Int
  val threshold: Int
  var options: Int
  val showSystemApps: Boolean
  val thresholdChanges: Flow<Int>

  suspend fun setThreshold(threshold: Int)
}
