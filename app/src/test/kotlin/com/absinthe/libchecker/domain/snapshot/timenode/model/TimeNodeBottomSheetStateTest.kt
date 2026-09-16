package com.absinthe.libchecker.domain.snapshot.timenode.model

import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TimeNodeBottomSheetStateTest {

  @Test
  fun `list and auto remove updates preserve the rest of the sheet state`() {
    val initial = TimeNodeBottomSheetState(
      title = "Snapshots",
      header = TimeNodeHeaderState.AutoRemove(threshold = -1)
    )
    val listData = SnapshotTimeNodeListData(
      items = listOf(
        SnapshotTimeNodeItem(
          timestamp = 123L,
          timestampText = "Snapshot 123",
          description = "Snapshot 123",
          topAppPackageNames = listOf("sample.package")
        )
      ),
      packageIconSources = emptyMap()
    )

    val loaded = initial.withListData(listData)
    val autoRemoveEnabled = loaded.withAutoRemoveThreshold(threshold = 5)

    assertSame(listData, loaded.listData)
    assertEquals("Snapshots", autoRemoveEnabled.title)
    assertEquals(TimeNodeHeaderState.AutoRemove(threshold = 5), autoRemoveEnabled.header)
    assertSame(listData, autoRemoveEnabled.listData)

    val removed = autoRemoveEnabled.removeItemAt(position = 0)
    assertEquals(emptyList<SnapshotTimeNodeItem>(), removed.listData.items)
    assertSame(listData.packageIconSources, removed.listData.packageIconSources)
    assertSame(removed, removed.removeItemAt(position = 0))
  }

  @Test
  fun `auto remove threshold does not mutate comparison header`() {
    val state = TimeNodeBottomSheetState(
      title = "Compare",
      header = TimeNodeHeaderState.AddApk(isLeft = true)
    )

    assertSame(state, state.withAutoRemoveThreshold(threshold = 3))
  }

  @Test
  fun `withSelectedTile switches selectedDate to clicked tile and selects matching snapshot if present`() {
    val initial = TimeNodeBottomSheetState(
      title = "Snapshots",
      header = TimeNodeHeaderState.AutoRemove(threshold = -1),
      listData = SnapshotTimeNodeListData(
        items = listOf(
          SnapshotTimeNodeItem(
            timestamp = 1000L,
            timestampText = "Snapshot 1000",
            description = "Snapshot 1000",
            topAppPackageNames = emptyList(),
            isSelected = false
          )
        ),
        packageIconSources = emptyMap()
      ),
      selectedTimestamp = null,
      selectedDate = null
    )

    val nonSnapshotDayTile = DayContribution(
      date = LocalDate.of(2026, 9, 5),
      updateCount = 3,
      snapshotTimestamp = 1000L,
      isSnapshotDay = false
    )

    val tileSelected = initial.withSelectedTile(nonSnapshotDayTile)
    assertEquals(LocalDate.of(2026, 9, 5), tileSelected.selectedDate)
    assertEquals(1000L, tileSelected.selectedTimestamp)
    assertEquals(true, tileSelected.listData.items.first().isSelected)

    val unassociatedTile = DayContribution(
      date = LocalDate.of(2026, 8, 1),
      updateCount = 0,
      snapshotTimestamp = null,
      isSnapshotDay = false
    )

    val unassociatedSelected = tileSelected.withSelectedTile(unassociatedTile)
    assertEquals(LocalDate.of(2026, 8, 1), unassociatedSelected.selectedDate)
    assertEquals(false, unassociatedSelected.listData.items.first().isSelected)
  }

  @Test
  fun `withSelectedTimestamp updates selectedDate to the matching date`() {
    val zone = ZoneId.of("UTC")
    val state = TimeNodeBottomSheetState(
      title = "Snapshots",
      header = TimeNodeHeaderState.AutoRemove(threshold = -1),
      listData = SnapshotTimeNodeListData(
        items = listOf(
          SnapshotTimeNodeItem(
            timestamp = 1725148800000L, // 2024-09-01T00:00:00Z
            timestampText = "Snapshot",
            description = "Snapshot",
            topAppPackageNames = emptyList()
          )
        ),
        packageIconSources = emptyMap()
      )
    )

    val updated = state.withSelectedTimestamp(1725148800000L, zone)
    assertEquals(1725148800000L, updated.selectedTimestamp)
    assertEquals(LocalDate.of(2024, 9, 1), updated.selectedDate)
    assertEquals(true, updated.listData.items.first().isSelected)
  }

  @Test
  fun `removing a snapshot clears stale contributions and selects a remaining snapshot`() {
    val date = LocalDate.now()
    val state = TimeNodeBottomSheetState(
      title = "Snapshots",
      header = TimeNodeHeaderState.AutoRemove(-1),
      listData = SnapshotTimeNodeListData(
        items = listOf(2000L, 1000L).map {
          SnapshotTimeNodeItem(it, "Snapshot", "Snapshot", emptyList(), isSelected = it == 2000L)
        },
        packageIconSources = emptyMap()
      ),
      contributionData = SnapshotContributionData(
        days = mapOf(date to DayContribution(date, snapshotTimestamp = 2000L)),
        startDate = date,
        endDate = date,
        snapshotColors = emptyMap(),
        totalUpdates = 0
      ),
      selectedTimestamp = 2000L,
      selectedDate = date
    )

    val remaining = state.removeItemAt(0)
    assertNull(remaining.contributionData)
    assertEquals(1000L, remaining.selectedTimestamp)
    assertEquals(true, remaining.listData.items.single().isSelected)
    val empty = remaining.removeItemAt(0)
    assertNull(empty.selectedTimestamp)
    assertNull(empty.selectedDate)
  }
}
