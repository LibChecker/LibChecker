package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.DEX
import com.chad.library.adapter.base.entity.node.BaseNode

sealed interface SnapshotDetailNodeLongClickAction {
  data class OpenReference(
    val target: SnapshotReferenceNavigationTarget
  ) : SnapshotDetailNodeLongClickAction
}

fun BaseNode.longClickAction(ownerPackageName: String): SnapshotDetailNodeLongClickAction? {
  return when (this) {
    is BaseSnapshotNode -> {
      if (item.itemType == DEX) {
        null
      } else {
        referenceTarget(ownerPackageName)?.let(
          SnapshotDetailNodeLongClickAction::OpenReference
        )
      }
    }

    else -> null
  }
}
