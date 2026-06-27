package com.absinthe.libchecker.domain.snapshot.timenode.model

data class SnapshotTimeNodeItem(
  val timestamp: Long,
  val topAppPackageNames: List<String>
)
