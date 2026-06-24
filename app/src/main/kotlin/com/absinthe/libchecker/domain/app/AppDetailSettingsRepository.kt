package com.absinthe.libchecker.domain.app

interface AppDetailSettingsRepository {
  val sortMode: Int
  val processMode: Boolean

  fun setSortMode(sortMode: Int)
  fun setProcessMode(enabled: Boolean)
}
