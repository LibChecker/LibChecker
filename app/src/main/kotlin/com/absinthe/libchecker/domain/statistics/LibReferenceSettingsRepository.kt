package com.absinthe.libchecker.domain.statistics

interface LibReferenceSettingsRepository {
  val threshold: Int
  val options: Int
  val showSystemApps: Boolean
}
