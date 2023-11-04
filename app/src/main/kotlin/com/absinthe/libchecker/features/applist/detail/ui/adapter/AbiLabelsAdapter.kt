package com.absinthe.libchecker.features.applist.detail.ui.adapter

import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.AbiLabelNode
import com.absinthe.libchecker.features.applist.detail.ui.adapter.provider.ABI_LABEL_PROVIDER
import com.absinthe.libchecker.features.applist.detail.ui.adapter.provider.AbiLabelProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class AbiLabelsAdapter : BaseNodeAdapter() {

  init {
    addNodeProvider(AbiLabelProvider())
  }

  override fun getItemType(data: List<BaseNode>, position: Int): Int {
    return when (data[position]) {
      is AbiLabelNode -> ABI_LABEL_PROVIDER
      else -> throw IllegalArgumentException("wrong abi label provider item type")
    }
  }
}
