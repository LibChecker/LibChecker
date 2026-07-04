package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.chad.library.adapter.base.entity.node.BaseNode

sealed interface SnapshotDetailNodeLongClickAction {
  data class OpenReference(
    val target: SnapshotReferenceNavigationTarget
  ) : SnapshotDetailNodeLongClickAction
}

fun BaseNode.longClickAction(ownerPackageName: String): SnapshotDetailNodeLongClickAction? {
  return when (this) {
    is BaseSnapshotNode -> referenceTarget(ownerPackageName)?.let(
      SnapshotDetailNodeLongClickAction::OpenReference
    )

    else -> null
  }
}
