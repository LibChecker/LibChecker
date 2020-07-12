package com.absinthe.libchecker.recyclerview.adapter.snapshot.node

import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotTitleNode(override val childNode: MutableList<BaseNode>, val title: String) : BaseExpandNode() {

    init {
        isExpanded = true
    }

}