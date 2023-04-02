package com.absinthe.libchecker.recyclerview.adapter.detail

import com.absinthe.libchecker.recyclerview.adapter.detail.node.AbiLabelNode
import com.absinthe.libchecker.recyclerview.adapter.detail.provider.ABI_LABEL_PROVIDER
import com.absinthe.libchecker.recyclerview.adapter.detail.provider.AbiLabelProvider
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
