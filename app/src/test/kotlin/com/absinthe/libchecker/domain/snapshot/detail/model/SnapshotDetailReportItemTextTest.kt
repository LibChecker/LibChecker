package com.absinthe.libchecker.domain.snapshot.detail.model

import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailReportItemTextTest {

  @Test
  fun buildsComponentReportLine() {
    assertEquals(
      "🔵<-> com.example.SyncService\n",
      buildSnapshotDetailReportItemText(
        SnapshotDetailItem(
          name = "com.example.SyncService",
          title = "com.example.SyncService",
          extra = "",
          diffType = MOVED,
          itemType = SERVICE
        )
      )
    )
  }

  @Test
  fun buildsNativeReportLineWithExtra() {
    assertEquals(
      "🟡~ libfoo.so\n\t12 KB → 14 KB\n",
      buildSnapshotDetailReportItemText(
        SnapshotDetailItem(
          name = "libfoo.so",
          title = "libfoo.so",
          extra = "12 KB → 14 KB",
          diffType = CHANGED,
          itemType = NATIVE
        )
      )
    )
  }

  @Test
  fun buildsDexReportLineWithMetrics() {
    assertEquals(
      "🟡~ classes.dex\n\t10 KB → 12 KB\n+2 KB, +20.0%\n",
      buildSnapshotDetailReportItemText(
        SnapshotDetailItem(
          name = "base/classes.dex",
          title = "classes.dex",
          extra = "10 KB → 12 KB\n+2 KB, +20.0%",
          diffType = CHANGED,
          itemType = DEX
        )
      )
    )
  }
}
