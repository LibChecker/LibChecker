package com.absinthe.libchecker.recyclerview.diff

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.bean.LibStringItemChip

class LibStringDiffUtil : DiffUtil.ItemCallback<LibStringItemChip>() {

  override fun areItemsTheSame(oldItem: LibStringItemChip, newItem: LibStringItemChip): Boolean {
    return oldItem.item.name == newItem.item.name
  }

  override fun areContentsTheSame(
    oldItem: LibStringItemChip,
    newItem: LibStringItemChip,
  ): Boolean {
    return oldItem.item == newItem.item && oldItem.chip == newItem.chip
  }
}
