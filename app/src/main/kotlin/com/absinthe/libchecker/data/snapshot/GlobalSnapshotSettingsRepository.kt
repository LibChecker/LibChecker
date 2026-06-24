package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository

class GlobalSnapshotSettingsRepository : SnapshotSettingsRepository {
  override var options: Int
    get() = GlobalValues.snapshotOptions
    set(value) {
      GlobalValues.snapshotOptions = value
    }
}
