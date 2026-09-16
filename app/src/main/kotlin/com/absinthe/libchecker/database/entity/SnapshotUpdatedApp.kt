package com.absinthe.libchecker.database.entity

data class SnapshotUpdatedApp(
  val packageName: String,
  val label: String,
  val lastUpdatedTime: Long
)
