package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDetailAdapterAnimationInstrumentedTest {

  @Test
  fun toggleSectionEmitsRangeUpdatesThatDriveRecyclerViewItemAnimations() {
    val items = listOf(buildItem("com.example.SyncService"), buildItem("com.example.UploadService"))
    val section = SnapshotDetailSection(
      type = SERVICE,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services expanded",
      collapsedDescription = "Services collapsed",
      items = items,
      statusCounts = emptyList()
    )
    val adapter = SnapshotDetailAdapter()
    var fullRefreshCount = 0
    val insertedRanges = mutableListOf<Pair<Int, Int>>()
    val removedRanges = mutableListOf<Pair<Int, Int>>()
    adapter.registerAdapterDataObserver(
      object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
          fullRefreshCount++
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
          insertedRanges += positionStart to itemCount
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
          removedRanges += positionStart to itemCount
        }
      }
    )
    adapter.submitSections(listOf(section))
    fullRefreshCount = 0

    adapter.toggleSectionAt(0)

    assertEquals(0, fullRefreshCount)
    assertEquals(listOf(1 to 2), removedRanges)

    adapter.toggleSectionAt(0)

    assertEquals(0, fullRefreshCount)
    assertEquals(listOf(1 to 2), insertedRanges)
  }

  private fun buildItem(name: String): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = ADDED,
        itemType = SERVICE
      ),
      title = name,
      extra = "",
      description = name,
      reportText = "$name\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 1,
        colorRes = 2,
        labelRes = 3
      ),
      ruleChip = null
    )
  }
}
