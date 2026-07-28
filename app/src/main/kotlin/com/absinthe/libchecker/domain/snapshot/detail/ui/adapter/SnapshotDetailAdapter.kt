package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.providerViewType
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider.SnapshotDetailItemProvider
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider.SnapshotTitleProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotDetailAdapter : BaseNodeAdapter() {

  init {
    addNodeProvider(SnapshotTitleProvider())
    addNodeProvider(SnapshotDetailItemProvider())
  }

  override fun getItemType(data: List<BaseNode>, position: Int): Int {
    return data[position].providerViewType
  }
}
