package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.ui.app.MenuOptionItem

data class SnapshotMenuBottomSheetState(
  val demoDisplayData: SnapshotItemDisplayData,
  val options: List<MenuOptionItem>
)

sealed interface SnapshotMenuAction {
  data class OptionChanged(
    val item: MenuOptionItem,
    val isChecked: Boolean
  ) : SnapshotMenuAction
}

fun buildSnapshotMenuBottomSheetState(
  currentOptions: Int,
  demoDisplayData: SnapshotItemDisplayData,
  includeIecUnits: Boolean
): SnapshotMenuBottomSheetState {
  val options = buildList {
    add(MenuOptionItem(R.string.snapshot_menu_show_update_time, SnapshotOptions.SHOW_UPDATE_TIME, currentOptions))
    add(
      MenuOptionItem(
        R.string.snapshot_menu_hide_no_component_changes,
        SnapshotOptions.HIDE_NO_COMPONENT_CHANGES,
        currentOptions
      )
    )
    add(MenuOptionItem(R.string.snapshot_menu_diff_highlight, SnapshotOptions.DIFF_HIGHLIGHT, currentOptions))
    add(MenuOptionItem(R.string.snapshot_menu_diff_emphasis, SnapshotOptions.DIFF_EMPHASIS, currentOptions))
    if (includeIecUnits) {
      add(MenuOptionItem(R.string.snapshot_menu_use_iec_units, SnapshotOptions.USE_IEC_UNITS, currentOptions))
    }
  }
  return SnapshotMenuBottomSheetState(
    demoDisplayData = demoDisplayData,
    options = options
  )
}
