package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/27
 * </pre>
 */
data class SnapshotDetailCountNode(
  val diffType: Int,
  val count: Int,
  val countText: String,
  val status: SnapshotDetailItemStatusDisplayData
)
