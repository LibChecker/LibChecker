package com.absinthe.libchecker.domain.statistics

interface LibReferenceSettingsRepository {
  val appListDisplayOptions: Int
  val threshold: Int
  var options: Int
  val showSystemApps: Boolean

  suspend fun setThreshold(threshold: Int)
}
