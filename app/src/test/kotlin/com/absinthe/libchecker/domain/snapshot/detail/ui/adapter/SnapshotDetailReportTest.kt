package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailReportTest {

  @Test
  fun concatenatesHeaderAndExpandedItemReportsInVisibleOrder() {
    val serviceSection = buildSection(
      type = SERVICE,
      title = "Services",
      items = listOf(
        buildItem(
          name = "com.example.SyncService",
          itemType = SERVICE,
          reportText = "service report 1\n"
        ),
        buildItem(
          name = "com.example.UploadService",
          itemType = SERVICE,
          reportText = "service report 2\n"
        )
      )
    )
    val nativeSection = buildSection(
      type = NATIVE,
      title = "Native libraries",
      items = listOf(
        buildItem(
          name = "libfoo.so",
          itemType = NATIVE,
          reportText = "native report\n"
        )
      )
    )

    assertEquals(
      "[Services]\nservice report 1\nservice report 2\n[Native libraries]\nnative report\n",
      buildSnapshotDetailVisibleReportText(
        buildSnapshotDetailRows(listOf(serviceSection, nativeSection))
      )
    )
  }

  @Test
  fun omitsCollapsedSectionsItemReports() {
    val serviceSection = buildSection(
      type = SERVICE,
      title = "Services",
      items = listOf(
        buildItem(
          name = "com.example.SyncService",
          itemType = SERVICE,
          reportText = "service report\n"
        )
      )
    )
    val nativeSection = buildSection(
      type = NATIVE,
      title = "Native libraries",
      items = listOf(
        buildItem(
          name = "libfoo.so",
          itemType = NATIVE,
          reportText = "native report\n"
        )
      )
    )

    assertEquals(
      "[Services]\n[Native libraries]\nnative report\n",
      buildSnapshotDetailVisibleReportText(
        buildSnapshotDetailRows(
          sections = listOf(serviceSection, nativeSection),
          collapsedTypes = setOf(SERVICE)
        )
      )
    )
  }

  private fun buildSection(
    type: Int,
    title: String,
    items: List<SnapshotDetailItemDisplayData>
  ): SnapshotDetailSection {
    return SnapshotDetailSection(
      type = type,
      title = title,
      reportText = "[$title]\n",
      expandedDescription = "$title expanded",
      collapsedDescription = "$title collapsed",
      items = items,
      statusCounts = listOf(
        SnapshotDetailStatusCount(
          diffType = if (type == SERVICE) ADDED else MOVED,
          count = items.size,
          countText = items.size.toString(),
          label = if (type == SERVICE) "Added" else "Moved",
          status = SnapshotDetailItemStatusDisplayData(
            iconRes = 10,
            colorRes = 11,
            labelRes = 12
          )
        )
      )
    )
  }

  private fun buildItem(
    name: String,
    itemType: Int,
    reportText: String
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = ADDED,
        itemType = itemType
      ),
      title = name,
      extra = "",
      description = name,
      reportText = reportText,
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 20,
        colorRes = 21,
        labelRes = 22
      ),
      ruleChip = null
    )
  }
}
