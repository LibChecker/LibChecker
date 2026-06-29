package com.absinthe.libchecker.domain.statistics.reference.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.chad.library.adapter.base.entity.node.BaseNode

class RefListDiffUtil : DiffUtil.ItemCallback<BaseNode>() {

  override fun areItemsTheSame(oldItem: BaseNode, newItem: BaseNode): Boolean {
    return if (oldItem is LibReference && newItem is LibReference) {
      oldItem.libName == newItem.libName && oldItem.type == newItem.type
    } else {
      oldItem == newItem
    }
  }

  override fun areContentsTheSame(oldItem: BaseNode, newItem: BaseNode): Boolean {
    return if (oldItem is LibReference && newItem is LibReference) {
      oldItem.libName == newItem.libName &&
        oldItem.referredList.size == newItem.referredList.size &&
        oldItem.referredList === newItem.referredList &&
        oldItem.rule == newItem.rule &&
        oldItem.type == newItem.type
    } else {
      oldItem == newItem
    }
  }
}
