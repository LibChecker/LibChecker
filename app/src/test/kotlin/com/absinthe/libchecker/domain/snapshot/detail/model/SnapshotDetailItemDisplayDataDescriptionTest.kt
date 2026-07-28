package com.absinthe.libchecker.domain.snapshot.detail.model

import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailItemDisplayDataDescriptionTest {

  @Test
  fun followsVisualStatusRuleTitleAndExtraOrder() {
    assertEquals(
      "Changed, JNI, libfoo.so, 42 KB",
      buildSnapshotDetailItemDescription(
        statusLabel = "Changed",
        title = "libfoo.so",
        extra = "42 KB",
        ruleLabel = "JNI"
      )
    )
  }

  @Test
  fun skipsBlankParts() {
    assertEquals(
      "Added, com.example.MainActivity",
      buildSnapshotDetailItemDescription(
        statusLabel = "Added",
        title = "com.example.MainActivity",
        extra = " ",
        ruleLabel = null
      )
    )
  }

  @Test
  fun joinsSectionTitleCountsAndExpansionState() {
    assertEquals(
      "Native libraries, Added 1, Changed 1, Expanded",
      buildSnapshotDetailSectionDescription(
        title = "Native libraries",
        statusCounts = listOf(
          SnapshotDetailStatusCount(
            diffType = ADDED,
            count = 1,
            countText = "1",
            label = "Added",
            status = SnapshotDetailItemStatusDisplayData(0, 0, 0)
          ),
          SnapshotDetailStatusCount(
            diffType = CHANGED,
            count = 1,
            countText = "1",
            label = "Changed",
            status = SnapshotDetailItemStatusDisplayData(0, 0, 0)
          )
        ),
        expansionStateLabel = "Expanded"
      )
    )
  }
}
