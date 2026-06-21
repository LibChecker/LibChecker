package com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.chad.library.adapter.base.entity.node.BaseNode

open class BaseSnapshotNode(val item: SnapshotDetailItem) : BaseNode() {

  override val childNode: MutableList<BaseNode>? = null
}
