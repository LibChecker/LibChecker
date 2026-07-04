package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.chad.library.adapter.base.entity.node.BaseNode

sealed interface SnapshotDetailNodeClickAction {
  data object ToggleSection : SnapshotDetailNodeClickAction

  data class OpenDetail(
    val target: SnapshotDetailNavigationTarget
  ) : SnapshotDetailNodeClickAction
}

val BaseNode.clickAction: SnapshotDetailNodeClickAction?
  get() = when (this) {
    is SnapshotTitleNode -> SnapshotDetailNodeClickAction.ToggleSection
    is BaseSnapshotNode -> detailTarget?.let(SnapshotDetailNodeClickAction::OpenDetail)
    else -> null
  }
