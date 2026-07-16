package com.absinthe.libchecker.domain.snapshot.timenode.model

data class SnapshotTimeNodeItem(
  val timestamp: Long,
  val timestampText: String,
  val description: String,
  val topAppPackageNames: List<String>
)
