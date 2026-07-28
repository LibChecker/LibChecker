package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.chad.library.adapter.base.entity.node.BaseNode
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailNodeViewTypeTest {

  @Test
  fun returnsTitleProviderTypeForTitleNode() {
    val node = SnapshotTitleNode(
      childNode = mutableListOf(),
      type = NATIVE,
      title = "Native libraries",
      reportText = "[Native libraries]\n",
      expandedDescription = "Native libraries, Moved 1, Expanded",
      collapsedDescription = "Native libraries, Moved 1, Collapsed",
      counts = emptyList()
    )

    assertEquals(SNAPSHOT_TITLE_PROVIDER, node.providerViewType)
  }

  @Test
  fun returnsNativeProviderTypeForNativeNode() {
    val node = SnapshotNativeNode(buildDisplayData(type = NATIVE))

    assertEquals(SNAPSHOT_ITEM_PROVIDER, node.providerViewType)
  }

  @Test
  fun returnsComponentProviderTypeForComponentNode() {
    val node = SnapshotComponentNode(buildDisplayData(type = SERVICE))

    assertEquals(SNAPSHOT_ITEM_PROVIDER, node.providerViewType)
  }

  @Test(expected = IllegalArgumentException::class)
  fun throwsForUnsupportedNode() {
    object : BaseNode() {
      override val childNode: MutableList<BaseNode>? = null
    }.providerViewType
  }

  private fun buildDisplayData(type: Int): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = "libexample.so",
        title = "libexample.so",
        extra = "",
        diffType = MOVED,
        itemType = type
      ),
      title = "libexample.so",
      extra = "",
      description = "libexample.so",
      reportText = "native report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 0,
        colorRes = 0,
        labelRes = 0
      ),
      ruleChip = null
    )
  }
}
