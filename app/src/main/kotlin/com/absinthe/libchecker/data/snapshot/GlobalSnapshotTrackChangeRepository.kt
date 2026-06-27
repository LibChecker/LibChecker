package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository

class GlobalSnapshotTrackChangeRepository : SnapshotTrackChangeRepository {

  override fun markChanged() {
    GlobalValues.trackItemsChanged = true
  }

  override fun consumeChanged(): Boolean {
    return GlobalValues.trackItemsChanged.also {
      GlobalValues.trackItemsChanged = false
    }
  }
}
