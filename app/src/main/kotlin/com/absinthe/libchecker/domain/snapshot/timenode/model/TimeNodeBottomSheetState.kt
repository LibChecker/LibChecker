package com.absinthe.libchecker.domain.snapshot.timenode.model

import android.graphics.Rect
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TimeNodeBottomSheetState(
  val title: String,
  val header: TimeNodeHeaderState,
  val listData: SnapshotTimeNodeListData = SnapshotTimeNodeListData(
    items = emptyList(),
    packageIconSources = emptyMap()
  ),
  val contributionData: SnapshotContributionData? = null,
  val selectedTimestamp: Long? = null,
  val selectedDate: LocalDate? = null
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

  data class SelectTile(
    val dayContribution: DayContribution,
    val anchorRect: Rect
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
  if (selectedTimestamp == null) {
    return copy(listData = listData)
  }
  val updatedItems = listData.items.map {
    it.copy(
      isSelected = it.timestamp == selectedTimestamp,
      isCurrent = it.timestamp == selectedTimestamp
    )
  }
  return copy(listData = listData.copy(items = updatedItems))
}

fun TimeNodeBottomSheetState.withContributionData(
  contributionData: SnapshotContributionData
): TimeNodeBottomSheetState {
  val colors = contributionData.snapshotColors
  val updatedItems = listData.items.map {
    it.copy(tagColor = colors[it.timestamp])
  }
  return copy(
    contributionData = contributionData,
    listData = listData.copy(items = updatedItems)
  )
}

fun TimeNodeBottomSheetState.withSelectedTimestamp(
  timestamp: Long?,
  zoneId: ZoneId = ZoneId.systemDefault()
): TimeNodeBottomSheetState {
  val updatedItems = listData.items.map {
    it.copy(
      isSelected = it.timestamp == timestamp,
      isCurrent = it.timestamp == timestamp
    )
  }
  val targetDate = timestamp?.let {
    Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
  }
  return copy(
    selectedTimestamp = timestamp,
    selectedDate = targetDate ?: selectedDate,
    listData = listData.copy(items = updatedItems)
  )
}

fun TimeNodeBottomSheetState.withSelectedTile(
  dayContribution: DayContribution
): TimeNodeBottomSheetState {
  return copy(
    selectedDate = dayContribution.date
  )
}

fun TimeNodeBottomSheetState.withAutoRemoveThreshold(threshold: Int): TimeNodeBottomSheetState {
  val header = header as? TimeNodeHeaderState.AutoRemove ?: return this
  return copy(header = header.copy(threshold = threshold))
}

fun TimeNodeBottomSheetState.removeItemAt(position: Int): TimeNodeBottomSheetState {
  if (position !in listData.items.indices) {
    return this
  }
  val remaining = listData.items.toMutableList().apply { removeAt(position) }
  val selected = selectedTimestamp?.takeIf { timestamp -> remaining.any { it.timestamp == timestamp } }
    ?: remaining.firstOrNull()?.timestamp
  return copy(
    contributionData = null,
    selectedDate = null,
    listData = listData.copy(items = remaining)
  ).withSelectedTimestamp(selected)
}
