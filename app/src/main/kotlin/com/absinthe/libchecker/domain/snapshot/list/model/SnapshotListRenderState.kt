package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource

data class SnapshotListRenderState(
  val displayOptions: SnapshotListDisplayOptions = SnapshotListDisplayOptions(),
  val packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap(),
  val apexPackageNames: Set<String> = emptySet(),
  val highlightText: String = ""
)
