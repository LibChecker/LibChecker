package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.LibType
import com.chad.library.adapter.base.entity.node.BaseNode

data class SnapshotDetailLibraryDialogTarget(
  val name: String,
  @LibType val type: Int,
  val regexName: String?
)

sealed interface SnapshotDetailNodeChipClickAction {
  data class OpenLibraryDetail(
    val target: SnapshotDetailLibraryDialogTarget
  ) : SnapshotDetailNodeChipClickAction
}

val BaseNode.chipClickAction: SnapshotDetailNodeChipClickAction?
  get() = when (this) {
    is BaseSnapshotNode -> displayData.ruleChip?.let { ruleChip ->
      SnapshotDetailNodeChipClickAction.OpenLibraryDetail(
        SnapshotDetailLibraryDialogTarget(
          name = displayData.item.name,
          type = displayData.item.itemType,
          regexName = ruleChip.regexName
        )
      )
    }

    else -> null
  }
