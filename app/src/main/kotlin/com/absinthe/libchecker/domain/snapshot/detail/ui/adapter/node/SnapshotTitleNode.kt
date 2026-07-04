package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.LibType
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotTitleNode(
  override val childNode: MutableList<BaseNode>,
  @LibType val type: Int,
  val counts: List<SnapshotDetailCountNode>
) : BaseExpandNode() {

  init {
    isExpanded = true
  }
}
