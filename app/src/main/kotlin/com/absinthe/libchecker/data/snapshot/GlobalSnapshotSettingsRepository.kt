package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository

class GlobalSnapshotSettingsRepository : SnapshotSettingsRepository {
  override var options: Int
    get() = GlobalValues.snapshotOptions
    set(value) {
      GlobalValues.snapshotOptions = value
    }

  override var autoRemoveThreshold: Int
    get() = GlobalValues.snapshotAutoRemoveThreshold
    set(value) {
      GlobalValues.snapshotAutoRemoveThreshold = value
    }

  override var keepRule: String
    get() = GlobalValues.snapshotKeep
    set(value) {
      GlobalValues.snapshotKeep = value
    }

  override val listDisplayOptions: SnapshotListDisplayOptions
    get() = SnapshotListDisplayOptions(
      highlightDiffs = (GlobalValues.snapshotOptions and SnapshotOptions.DIFF_HIGHLIGHT) > 0,
      emphasizeDiffs = (GlobalValues.snapshotOptions and SnapshotOptions.DIFF_EMPHASIS) > 0,
      showUpdateTime = (GlobalValues.snapshotOptions and SnapshotOptions.SHOW_UPDATE_TIME) > 0,
      tintAbiLabels = (GlobalValues.advancedOptions and AdvancedOptions.TINT_ABI_LABEL) > 0
    )
}
