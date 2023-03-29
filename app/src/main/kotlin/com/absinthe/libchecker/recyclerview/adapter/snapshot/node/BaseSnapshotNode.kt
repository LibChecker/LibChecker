package com.absinthe.libchecker.recyclerview.adapter.snapshot.node

import com.absinthe.libchecker.model.SnapshotDetailItem
import com.chad.library.adapter.base.entity.node.BaseNode

open class BaseSnapshotNode(val item: SnapshotDetailItem) : BaseNode() {

  override val childNode: MutableList<BaseNode>? = null
}
