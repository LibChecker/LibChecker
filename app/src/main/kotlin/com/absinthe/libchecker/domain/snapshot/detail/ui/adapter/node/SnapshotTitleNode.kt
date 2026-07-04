package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.LibType
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotTitleNode(
  override val childNode: MutableList<BaseNode>,
  @LibType val type: Int,
  val title: String,
  override val reportText: String,
  val expandedDescription: String,
  val collapsedDescription: String,
  val counts: List<SnapshotDetailCountNode>
) : BaseExpandNode(),
  SnapshotReportNode {

  val contentDescription: String
    get() = if (isExpanded) {
      expandedDescription
    } else {
      collapsedDescription
    }

  init {
    isExpanded = true
  }
}
