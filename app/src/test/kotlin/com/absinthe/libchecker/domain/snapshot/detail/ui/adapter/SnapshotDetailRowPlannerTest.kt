package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailRowPlannerTest {

  @Test
  fun flattensSectionsAsHeaderThenVisibleItems() {
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
          diffType = CHANGED,
          reportText = "service report 2\n"
        )
      ),
      counts = listOf(
        buildStatusCount(diffType = ADDED, count = 1, countText = "1", label = "Added"),
        buildStatusCount(diffType = CHANGED, count = 1, countText = "1", label = "Changed")
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
      ),
      counts = listOf(
        buildStatusCount(diffType = MOVED, count = 1, countText = "1", label = "Moved")
      )
    )

    assertEquals(
      listOf(
        SnapshotDetailRow.Header(section = serviceSection, expanded = true),
        SnapshotDetailRow.Item(sectionType = SERVICE, displayData = serviceSection.items[0]),
        SnapshotDetailRow.Item(sectionType = SERVICE, displayData = serviceSection.items[1]),
        SnapshotDetailRow.Header(section = nativeSection, expanded = true),
        SnapshotDetailRow.Item(sectionType = NATIVE, displayData = nativeSection.items[0])
      ),
      buildSnapshotDetailRows(listOf(serviceSection, nativeSection))
    )
  }

  @Test
  fun collapsingOneSectionTypeRemovesOnlyThatSectionsItemRows() {
    val serviceSection = buildSection(
      type = SERVICE,
      title = "Services",
      items = listOf(
        buildItem(
          name = "com.example.SyncService",
          itemType = SERVICE,
          reportText = "service report\n"
        )
      ),
      counts = listOf(
        buildStatusCount(diffType = ADDED, count = 1, countText = "1", label = "Added")
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
      ),
      counts = listOf(
        buildStatusCount(diffType = MOVED, count = 1, countText = "1", label = "Moved")
      )
    )

    assertEquals(
      listOf(
        SnapshotDetailRow.Header(section = serviceSection, expanded = false),
        SnapshotDetailRow.Header(section = nativeSection, expanded = true),
        SnapshotDetailRow.Item(sectionType = NATIVE, displayData = nativeSection.items[0])
      ),
      buildSnapshotDetailRows(
        sections = listOf(serviceSection, nativeSection),
        collapsedTypes = setOf(SERVICE)
      )
    )
  }

  private fun buildSection(
    type: Int,
    title: String,
    items: List<SnapshotDetailItemDisplayData>,
    counts: List<SnapshotDetailStatusCount>
  ): SnapshotDetailSection {
    return SnapshotDetailSection(
      type = type,
      title = title,
      reportText = "[$title]\n",
      expandedDescription = "$title expanded",
      collapsedDescription = "$title collapsed",
      items = items,
      statusCounts = counts
    )
  }

  private fun buildItem(
    name: String,
    itemType: Int,
    diffType: Int = ADDED,
    reportText: String
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = diffType,
        itemType = itemType
      ),
      title = name,
      extra = "",
      description = name,
      reportText = reportText,
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 10,
        colorRes = 11,
        labelRes = 12
      ),
      ruleChip = null
    )
  }

  private fun buildStatusCount(
    diffType: Int,
    count: Int,
    countText: String,
    label: String
  ): SnapshotDetailStatusCount {
    return SnapshotDetailStatusCount(
      diffType = diffType,
      count = count,
      countText = countText,
      label = label,
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 20 + diffType,
        colorRes = 30 + diffType,
        labelRes = 40 + diffType
      )
    )
  }
}
