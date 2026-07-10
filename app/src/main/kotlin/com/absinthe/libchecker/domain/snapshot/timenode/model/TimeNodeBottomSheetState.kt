package com.absinthe.libchecker.domain.snapshot.timenode.model

import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData

data class TimeNodeBottomSheetState(
  val title: String,
  val header: TimeNodeHeaderState,
  val listData: SnapshotTimeNodeListData = SnapshotTimeNodeListData(
    items = emptyList(),
    packageIconSources = emptyMap()
  )
)

sealed interface TimeNodeHeaderState {
  data class AutoRemove(
    val threshold: Int
  ) : TimeNodeHeaderState

  data class AddApk(
    val isLeft: Boolean
  ) : TimeNodeHeaderState
}

sealed interface TimeNodeBottomSheetAction {
  data class SelectItem(
    val position: Int,
    val item: SnapshotTimeNodeItem
  ) : TimeNodeBottomSheetAction

  data class AddApk(
    val isLeft: Boolean
  ) : TimeNodeBottomSheetAction

  data class SetAutoRemoveEnabled(
    val enabled: Boolean
  ) : TimeNodeBottomSheetAction
}

fun TimeNodeBottomSheetState.withListData(
  listData: SnapshotTimeNodeListData
): TimeNodeBottomSheetState {
  return copy(listData = listData)
}

fun TimeNodeBottomSheetState.withAutoRemoveThreshold(threshold: Int): TimeNodeBottomSheetState {
  val header = header as? TimeNodeHeaderState.AutoRemove ?: return this
  return copy(header = header.copy(threshold = threshold))
}

fun TimeNodeBottomSheetState.removeItemAt(position: Int): TimeNodeBottomSheetState {
  if (position !in listData.items.indices) {
    return this
  }
  return copy(
    listData = listData.copy(
      items = listData.items.toMutableList().apply {
        removeAt(position)
      }
    )
  )
}
