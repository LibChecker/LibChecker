package com.absinthe.libchecker.features.applist.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.database.entity.LCItem

class AppListDiffUtil : DiffUtil.ItemCallback<LCItem>() {

  override fun areItemsTheSame(oldItem: LCItem, newItem: LCItem): Boolean {
    return oldItem.packageName == newItem.packageName
  }

  override fun areContentsTheSame(oldItem: LCItem, newItem: LCItem): Boolean {
    return oldItem.label == newItem.label &&
      oldItem.abi == newItem.abi &&
      oldItem.versionName == newItem.versionName &&
      oldItem.lastUpdatedTime == newItem.lastUpdatedTime
  }
}
