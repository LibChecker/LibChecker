package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData

data class SnapshotDetailItemRenderState(
  val title: CharSequence,
  val extra: CharSequence,
  @DrawableRes val iconRes: Int,
  @ColorInt val backgroundColor: Int,
  val contentDescription: String,
  val ruleChip: SnapshotDetailRuleChipDisplayData?,
  val chipClickAction: SnapshotDetailNodeChipClickAction?
)

val BaseSnapshotNode.itemRenderState: SnapshotDetailItemRenderState
  get() = SnapshotDetailItemRenderState(
    title = displayData.title,
    extra = displayData.extra,
    iconRes = displayData.status.iconRes,
    backgroundColor = displayData.backgroundColor,
    contentDescription = displayData.description,
    ruleChip = displayData.ruleChip,
    chipClickAction = chipClickAction
  )
