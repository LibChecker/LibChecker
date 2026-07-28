package com.absinthe.libchecker.domain.snapshot.display

import androidx.annotation.DrawableRes

data class SnapshotAbiDisplayData(
  val old: SnapshotAbiDisplayItem,
  val new: SnapshotAbiDisplayItem?
)

data class SnapshotAbiDisplayItem(
  val text: String,
  @DrawableRes val badgeRes: Int?,
  val isMultiArch: Boolean
)
