package com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.SnapshotDetailItemDisplayData
import com.chad.library.adapter.base.entity.node.BaseNode

open class BaseSnapshotNode(
  val displayData: SnapshotDetailItemDisplayData
) : BaseNode() {

  val item = displayData.item

  override val childNode: MutableList<BaseNode>? = null
}
