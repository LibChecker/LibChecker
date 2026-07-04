package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.chad.library.adapter.base.entity.node.BaseNode

data class SnapshotDetailNavigationTarget(
  val refName: String,
  @LibType val refType: Int
)

data class SnapshotReferenceNavigationTarget(
  val refName: String,
  val label: String?,
  @LibType val refType: Int
)

open class BaseSnapshotNode(
  val displayData: SnapshotDetailItemDisplayData
) : BaseNode(),
  SnapshotReportNode {

  val item = displayData.item
  val referenceLabel: String?
    get() = displayData.ruleChip?.label
  val detailTarget: SnapshotDetailNavigationTarget?
    get() = if (item.diffType == REMOVED) {
      null
    } else {
      SnapshotDetailNavigationTarget(
        refName = item.name,
        refType = item.itemType
      )
    }
  override val reportText: String
    get() = displayData.reportText

  fun referenceTarget(ownerPackageName: String): SnapshotReferenceNavigationTarget? {
    return if (isComponentType(item.itemType) && item.name.startsWith(ownerPackageName)) {
      null
    } else {
      SnapshotReferenceNavigationTarget(
        refName = item.name,
        label = referenceLabel,
        refType = item.itemType
      )
    }
  }

  override val childNode: MutableList<BaseNode>? = null
}
