package com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.LibType
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotTitleNode(
  override val childNode: MutableList<BaseNode>,
  @LibType val type: Int
) : BaseExpandNode() {

  init {
    isExpanded = true
  }
}
