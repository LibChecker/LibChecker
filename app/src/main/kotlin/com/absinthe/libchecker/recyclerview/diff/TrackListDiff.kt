package com.absinthe.libchecker.recyclerview.diff

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.model.TrackListItem

class TrackListDiff : DiffUtil.ItemCallback<TrackListItem>() {

  override fun areItemsTheSame(oldItem: TrackListItem, newItem: TrackListItem): Boolean {
    return oldItem.packageName == newItem.packageName
  }

  override fun areContentsTheSame(oldItem: TrackListItem, newItem: TrackListItem): Boolean {
    return oldItem.label == newItem.label &&
      oldItem.switchState == newItem.switchState
  }
}
