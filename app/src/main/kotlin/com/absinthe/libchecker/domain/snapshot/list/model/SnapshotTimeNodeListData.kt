package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem

data class SnapshotTimeNodeListData(
  val items: List<SnapshotTimeNodeItem>,
  val packageIconSources: Map<String, SnapshotPackageIconSource>
)
