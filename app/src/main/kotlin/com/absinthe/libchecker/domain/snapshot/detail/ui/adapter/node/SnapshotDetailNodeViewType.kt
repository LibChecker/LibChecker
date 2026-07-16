package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.chad.library.adapter.base.entity.node.BaseNode

const val SNAPSHOT_TITLE_PROVIDER = 1
const val SNAPSHOT_NATIVE_PROVIDER = 2
const val SNAPSHOT_COMPONENT_PROVIDER = 3

val BaseNode.providerViewType: Int
  get() = when (this) {
    is SnapshotTitleNode -> SNAPSHOT_TITLE_PROVIDER
    is SnapshotNativeNode -> SNAPSHOT_NATIVE_PROVIDER
    is SnapshotComponentNode -> SNAPSHOT_COMPONENT_PROVIDER
    else -> throw IllegalArgumentException("wrong snapshot provider item type")
  }
