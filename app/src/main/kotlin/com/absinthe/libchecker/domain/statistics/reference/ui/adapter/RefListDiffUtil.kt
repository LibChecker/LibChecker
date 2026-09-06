package com.absinthe.libchecker.domain.statistics.reference.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference

class RefListDiffUtil : DiffUtil.ItemCallback<LibReference>() {

  override fun areItemsTheSame(oldItem: LibReference, newItem: LibReference): Boolean {
    return oldItem.libName == newItem.libName && oldItem.type == newItem.type
  }

  override fun areContentsTheSame(oldItem: LibReference, newItem: LibReference): Boolean {
    return oldItem.libName == newItem.libName &&
      oldItem.referredList == newItem.referredList &&
      oldItem.rule == newItem.rule &&
      oldItem.type == newItem.type
  }
}
