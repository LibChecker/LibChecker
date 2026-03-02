package com.absinthe.libchecker.features.snapshot.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem

class SnapshotDiffUtil : DiffUtil.ItemCallback<SnapshotDiffItem>() {

  override fun areItemsTheSame(oldItem: SnapshotDiffItem, newItem: SnapshotDiffItem): Boolean {
    return oldItem.packageName == newItem.packageName
  }

  override fun areContentsTheSame(oldItem: SnapshotDiffItem, newItem: SnapshotDiffItem): Boolean {
    return oldItem.updateTime == newItem.updateTime &&
      oldItem.labelDiff == newItem.labelDiff &&
      oldItem.versionNameDiff == newItem.versionNameDiff &&
      oldItem.versionCodeDiff == newItem.versionCodeDiff &&
      oldItem.targetApiDiff == newItem.targetApiDiff &&
      oldItem.abiDiff == newItem.abiDiff &&
      oldItem.added == newItem.added &&
      oldItem.removed == newItem.removed &&
      oldItem.changed == newItem.changed &&
      oldItem.moved == newItem.moved &&
      oldItem.newInstalled == newItem.newInstalled &&
      oldItem.deleted == newItem.deleted &&
      oldItem.isTrackItem == newItem.isTrackItem
  }
}
