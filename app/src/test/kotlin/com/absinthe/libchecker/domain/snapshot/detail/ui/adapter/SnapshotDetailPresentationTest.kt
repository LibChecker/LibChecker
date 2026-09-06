package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailPresentationTest {
  @Test
  fun preparesMovedPathWithoutChangingReportOrAccessibleDescription() {
    val item = SnapshotDetailItem(
      name = "com.example.new.SyncService",
      title = "com.example.old.SyncService → com.example.new.SyncService",
      extra = "",
      diffType = MOVED,
      itemType = SERVICE,
      previousName = "com.example.old.SyncService"
    )
    val display = SnapshotDetailItemDisplayData(
      item = item,
      title = item.title,
      extra = "",
      description = "Moved, ${item.title}",
      reportText = "service report",
      status = SnapshotDetailItemStatusDisplayData(30, 31, 32),
      ruleChip = null
    )
    assertEquals("com.example.old", display.previousPackagePath)
    assertEquals(item.title, display.title)
    assertEquals("Moved, ${item.title}", display.description)
    assertEquals("service report", display.reportText)
    assertNull(display.copy(item = item.copy(diffType = ADDED)).previousPackagePath)
    listOf(null, "", "Service", ".Service", "  .Service").forEach { previous ->
      assertNull(display.copy(item = item.copy(previousName = previous)).previousPackagePath)
    }
  }
}
