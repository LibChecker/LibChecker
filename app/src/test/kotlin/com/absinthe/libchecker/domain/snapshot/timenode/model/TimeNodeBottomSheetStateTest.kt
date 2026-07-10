package com.absinthe.libchecker.domain.snapshot.timenode.model

import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import org.junit.Assert.assertEquals
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
}
