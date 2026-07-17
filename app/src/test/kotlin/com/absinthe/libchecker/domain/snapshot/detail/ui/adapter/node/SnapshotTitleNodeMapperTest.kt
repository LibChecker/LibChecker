package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotTitleNodeMapperTest {

  @Test
  fun mapsSectionDataIntoTitleNode() {
    val status = SnapshotDetailItemStatusDisplayData(
      iconRes = 1,
      colorRes = 2,
      labelRes = 4
    )
    val section = buildSection(
      type = SERVICE,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services, Moved 2, Expanded",
      collapsedDescription = "Services, Moved 2, Collapsed",
      statusCounts = listOf(
        SnapshotDetailStatusCount(
          diffType = MOVED,
          count = 2,
          countText = "2",
          label = "Moved",
          status = status
        )
      )
    )

    val node = section.toSnapshotTitleNode()

    assertEquals(SERVICE, node.type)
    assertEquals("Services", node.title)
    assertEquals("[Services]\n", node.reportText)
    assertEquals("Services, Moved 2, Expanded", node.expandedDescription)
    assertEquals("Services, Moved 2, Collapsed", node.collapsedDescription)
    assertEquals(
      listOf(
        SnapshotDetailCountNode(
          diffType = MOVED,
          count = 2,
          countText = "2",
          status = status
        )
      ),
      node.counts
    )
  }

  @Test
  fun usesComponentNodesForComponentSections() {
    val node = buildSection(type = SERVICE).toSnapshotTitleNode()

    assertTrue(node.childNode.single() is SnapshotComponentNode)
  }

  @Test
  fun usesNativeNodesForBinarySections() {
    val nativeNode = buildSection(type = NATIVE).toSnapshotTitleNode()
    val metadataNode = buildSection(type = METADATA).toSnapshotTitleNode()

    assertTrue(nativeNode.childNode.single() is SnapshotNativeNode)
    assertTrue(metadataNode.childNode.single() is SnapshotNativeNode)
  }

  private fun buildSection(
    type: Int,
    title: String = "Section",
    reportText: String = "[Section]\n",
    expandedDescription: String = "Section, Expanded",
    collapsedDescription: String = "Section, Collapsed",
    statusCounts: List<SnapshotDetailStatusCount> = emptyList()
  ): SnapshotDetailSection {
    return SnapshotDetailSection(
      type = type,
      title = title,
      reportText = reportText,
      expandedDescription = expandedDescription,
      collapsedDescription = collapsedDescription,
      items = listOf(buildDisplayData(type = type)),
      statusCounts = statusCounts
    )
  }

  private fun buildDisplayData(type: Int): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = "com.example.SyncService",
        title = "com.example.SyncService",
        extra = "",
        diffType = MOVED,
        itemType = type
      ),
      title = "com.example.SyncService",
      extra = "",
      description = "com.example.SyncService",
      reportText = "component report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 0,
        colorRes = 0,
        labelRes = 0
      ),
      ruleChip = null
    )
  }
}
