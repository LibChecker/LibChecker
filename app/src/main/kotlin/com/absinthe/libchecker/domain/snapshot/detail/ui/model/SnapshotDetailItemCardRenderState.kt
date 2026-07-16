package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class SnapshotDetailItemCardRenderState(
  val title: CharSequence,
  val extra: CharSequence,
  @DrawableRes val iconRes: Int,
  @ColorInt val backgroundColor: Int,
  val contentDescription: String,
  val ruleChip: SnapshotDetailRuleChipRenderState?
)
