package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.chad.library.adapter.base.entity.node.BaseNode

const val SNAPSHOT_TITLE_PROVIDER = 1
const val SNAPSHOT_ITEM_PROVIDER = 2

val BaseNode.providerViewType: Int
  get() = when (this) {
    is SnapshotTitleNode -> SNAPSHOT_TITLE_PROVIDER
    is BaseSnapshotNode -> SNAPSHOT_ITEM_PROVIDER
    else -> throw IllegalArgumentException("wrong snapshot provider item type")
  }
