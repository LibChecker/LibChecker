package com.absinthe.libchecker.domain.snapshot.list.model

sealed interface SnapshotMenuLayoutItem {

  data class Demo(
    val displayData: SnapshotItemDisplayData
  ) : SnapshotMenuLayoutItem

  data object Options : SnapshotMenuLayoutItem
}

fun buildSnapshotMenuLayoutItems(demoDisplayData: SnapshotItemDisplayData): List<SnapshotMenuLayoutItem> {
  return listOf(
    SnapshotMenuLayoutItem.Demo(demoDisplayData),
    SnapshotMenuLayoutItem.Options
  )
}
