package com.absinthe.libchecker.features.album.track.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.domain.snapshot.TrackedAppListItem

class TrackListDiff : DiffUtil.ItemCallback<TrackedAppListItem>() {

  override fun areItemsTheSame(oldItem: TrackedAppListItem, newItem: TrackedAppListItem): Boolean {
    return oldItem.packageName == newItem.packageName
  }

  override fun areContentsTheSame(oldItem: TrackedAppListItem, newItem: TrackedAppListItem): Boolean {
    return oldItem.label == newItem.label &&
      oldItem.switchState == newItem.switchState
  }
}
