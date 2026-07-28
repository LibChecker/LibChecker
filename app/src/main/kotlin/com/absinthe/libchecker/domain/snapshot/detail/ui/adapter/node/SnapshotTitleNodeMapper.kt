package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.chad.library.adapter.base.entity.node.BaseNode

fun SnapshotDetailSection.toSnapshotTitleNode(): SnapshotTitleNode {
  val nodes = items.mapTo(mutableListOf<BaseNode>()) { item ->
    when (type) {
      NATIVE, METADATA -> SnapshotNativeNode(item)
      else -> SnapshotComponentNode(item)
    }
  }
  return SnapshotTitleNode(
    childNode = nodes,
    type = type,
    title = title,
    reportText = reportText,
    expandedDescription = expandedDescription,
    collapsedDescription = collapsedDescription,
    counts = statusCounts.map {
      SnapshotDetailCountNode(
        diffType = it.diffType,
        count = it.count,
        countText = it.countText,
        status = it.status
      )
    }
  )
}
