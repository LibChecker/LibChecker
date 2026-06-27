package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.chad.library.adapter.base.entity.node.BaseNode

open class BaseSnapshotNode(
  val displayData: SnapshotDetailItemDisplayData
) : BaseNode() {

  val item = displayData.item

  override val childNode: MutableList<BaseNode>? = null
}
