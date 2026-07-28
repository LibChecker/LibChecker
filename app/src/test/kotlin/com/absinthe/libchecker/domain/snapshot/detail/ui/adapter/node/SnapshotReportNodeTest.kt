package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotReportNodeTest {

  @Test
  fun exposesReportTextForSnapshotDetailNodes() {
    val nodes: List<SnapshotReportNode> = listOf(
      SnapshotTitleNode(
        childNode = mutableListOf(),
        type = SERVICE,
        title = "Services",
        reportText = "[Services]\n",
        expandedDescription = "Services, Moved 1, Expanded",
        collapsedDescription = "Services, Moved 1, Collapsed",
        counts = emptyList()
      ),
      SnapshotComponentNode(
        displayData = buildDisplayData(
          title = "com.example.SyncService",
          itemType = SERVICE,
          reportText = "component report\n"
        )
      ),
      SnapshotNativeNode(
        displayData = buildDisplayData(
          title = "libfoo.so",
          itemType = NATIVE,
          reportText = "native report\n"
        )
      )
    )

    assertEquals(
      listOf("[Services]\n", "component report\n", "native report\n"),
      nodes.map { it.reportText }
    )
  }

  private fun buildDisplayData(
    title: String,
    @LibType itemType: Int,
    reportText: String
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = title,
        title = title,
        extra = "",
        diffType = MOVED,
        itemType = itemType
      ),
      title = title,
      extra = "",
      description = title,
      reportText = reportText,
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 0,
        colorRes = 0,
        labelRes = 0
      ),
      ruleChip = null
    )
  }
}
