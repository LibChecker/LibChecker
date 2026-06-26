package com.absinthe.libchecker.domain.snapshot.detail

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem

class BuildSnapshotDetailSectionsUseCase {

  operator fun invoke(items: List<SnapshotDetailItem>): List<SnapshotDetailSection> {
    return orderedTypes.mapNotNull { type ->
      val sectionItems = items.filter { it.itemType == type }
      if (sectionItems.isEmpty()) {
        null
      } else {
        SnapshotDetailSection(type, sectionItems)
      }
    }
  }

  private companion object {
    val orderedTypes = listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA)
  }
}

data class SnapshotDetailSection(
  @LibType val type: Int,
  val items: List<SnapshotDetailItem>
)
