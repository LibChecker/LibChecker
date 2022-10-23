package com.absinthe.libchecker.recyclerview.adapter.snapshot

import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.SNAPSHOT_COMPONENT_PROVIDER
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.SNAPSHOT_NATIVE_PROVIDER
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.SNAPSHOT_TITLE_PROVIDER
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.SnapshotComponentProvider
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.SnapshotNativeProvider
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.SnapshotTitleProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotDetailAdapter : BaseNodeAdapter() {

  init {
    addNodeProvider(SnapshotTitleProvider())
    addNodeProvider(SnapshotNativeProvider())
    addNodeProvider(SnapshotComponentProvider())
  }

  override fun getItemType(data: List<BaseNode>, position: Int): Int {
    return when (data[position]) {
      is SnapshotTitleNode -> SNAPSHOT_TITLE_PROVIDER
      is SnapshotNativeNode -> SNAPSHOT_NATIVE_PROVIDER
      is SnapshotComponentNode -> SNAPSHOT_COMPONENT_PROVIDER
      else -> throw IllegalArgumentException("wrong snapshot provider item type")
    }
  }
}
