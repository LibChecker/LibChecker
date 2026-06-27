package com.absinthe.libchecker.features.applist.detail.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip

class LibStringDiffUtil : DiffUtil.ItemCallback<LibStringItemChip>() {

  override fun areItemsTheSame(oldItem: LibStringItemChip, newItem: LibStringItemChip): Boolean {
    return oldItem.item.name == newItem.item.name
  }

  override fun areContentsTheSame(
    oldItem: LibStringItemChip,
    newItem: LibStringItemChip
  ): Boolean {
    return oldItem.item == newItem.item &&
      oldItem.rule == newItem.rule &&
      oldItem.labels.size == newItem.labels.size &&
      oldItem.labels.indices.all { oldItem.labels[it] == newItem.labels[it] }
  }
}
