package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseSnapshotNodeTest {

  @Test
  fun exposesRuleChipLabelForReferenceNavigation() {
    val node = SnapshotComponentNode(
      buildDisplayData(
        ruleChip = SnapshotDetailRuleChipDisplayData(
          label = "Jetpack App Startup",
          iconRes = 0,
          regexName = null,
          isSimpleColorIcon = false,
          useColorfulIcon = false
        )
      )
    )

    assertEquals("Jetpack App Startup", node.referenceLabel)
  }

  @Test
  fun returnsNullReferenceLabelWithoutRuleChip() {
    val node = SnapshotComponentNode(buildDisplayData(ruleChip = null))

    assertNull(node.referenceLabel)
  }

  private fun buildDisplayData(
    ruleChip: SnapshotDetailRuleChipDisplayData?
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = "com.example.SyncService",
        title = "com.example.SyncService",
        extra = "",
        diffType = MOVED,
        itemType = SERVICE
      ),
      title = "com.example.SyncService",
      extra = "",
      description = "com.example.SyncService",
      reportText = "component report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 0,
        colorRes = 0,
        countColorRes = 0,
        labelRes = 0
      ),
      backgroundColor = 0,
      ruleChip = ruleChip
    )
  }
}
