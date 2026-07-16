package com.absinthe.libchecker.domain.snapshot.detail.model

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailSummaryTest {

  @Test
  fun aggregatesTotalAndStatusCountsAcrossSections() {
    val addedStatus = SnapshotDetailItemStatusDisplayData(1, 2, 3)
    val removedStatus = SnapshotDetailItemStatusDisplayData(4, 5, 6)
    val sections = listOf(
      section(
        type = SERVICE,
        counts = listOf(count(ADDED, 1, "Added", addedStatus))
      ),
      section(
        type = ACTIVITY,
        counts = listOf(
          count(ADDED, 1, "Added", addedStatus),
          count(REMOVED, 4, "Removed", removedStatus)
        )
      ),
      section(
        type = METADATA,
        counts = listOf(count(REMOVED, 1, "Removed", removedStatus))
      )
    )

    val summary = buildSnapshotDetailSummary(sections) { "$it changes" }

    assertEquals(7, summary.totalCount)
    assertEquals("7 changes", summary.totalCountText)
    assertEquals(listOf(ADDED, REMOVED), summary.statusCounts.map { it.diffType })
    assertEquals(listOf(2, 5), summary.statusCounts.map { it.count })
    assertEquals("7 changes, Added 2, Removed 5", summary.description)
  }

  private fun section(
    type: Int,
    counts: List<SnapshotDetailStatusCount>
  ): SnapshotDetailSection {
    return SnapshotDetailSection(
      type = type,
      title = "Section",
      reportText = "[Section]\n",
      expandedDescription = "Section, Expanded",
      collapsedDescription = "Section, Collapsed",
      items = emptyList(),
      statusCounts = counts
    )
  }

  private fun count(
    diffType: Int,
    count: Int,
    label: String,
    status: SnapshotDetailItemStatusDisplayData
  ): SnapshotDetailStatusCount {
    return SnapshotDetailStatusCount(
      diffType = diffType,
      count = count,
      countText = count.toString(),
      label = label,
      status = status
    )
  }
}
