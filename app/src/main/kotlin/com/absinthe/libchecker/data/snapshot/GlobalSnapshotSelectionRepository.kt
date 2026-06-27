package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelectionRepository

class GlobalSnapshotSelectionRepository : SnapshotSelectionRepository {
  override var currentTimestamp: Long
    get() = GlobalValues.snapshotTimestamp
    set(value) {
      GlobalValues.snapshotTimestamp = value
    }
}
