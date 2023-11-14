package com.absinthe.libchecker.features.snapshot.detail.bean

import com.absinthe.libchecker.annotation.LibType

const val ADDED = 0
const val REMOVED = 1
const val CHANGED = 2
const val MOVED = 3

data class SnapshotDetailItem(
  val name: String,
  val title: String,
  val extra: String,
  val diffType: Int,
  @LibType val itemType: Int
)
