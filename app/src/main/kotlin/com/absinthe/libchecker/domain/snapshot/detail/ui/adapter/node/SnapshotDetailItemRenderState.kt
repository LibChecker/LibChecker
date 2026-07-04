package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipIconStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipRenderState

data class SnapshotDetailItemRenderState(
  val title: CharSequence,
  val extra: CharSequence,
  @DrawableRes val iconRes: Int,
  @ColorInt val backgroundColor: Int,
  val contentDescription: String,
  val ruleChip: SnapshotDetailRuleChipRenderState?,
  val chipClickAction: SnapshotDetailNodeChipClickAction?
)

val BaseSnapshotNode.itemRenderState: SnapshotDetailItemRenderState
  get() = SnapshotDetailItemRenderState(
    title = displayData.title,
    extra = displayData.extra,
    iconRes = displayData.status.iconRes,
    backgroundColor = displayData.backgroundColor,
    contentDescription = displayData.description,
    ruleChip = displayData.ruleChip?.toRenderState(displayData.backgroundColor),
    chipClickAction = chipClickAction
  )

private fun SnapshotDetailRuleChipDisplayData.toRenderState(
  @ColorInt backgroundColor: Int
): SnapshotDetailRuleChipRenderState {
  return SnapshotDetailRuleChipRenderState(
    label = label,
    iconRes = iconRes,
    backgroundColor = backgroundColor,
    iconStyle = when {
      isSimpleColorIcon -> SnapshotDetailRuleChipIconStyle.BlackTint
      useColorfulIcon -> SnapshotDetailRuleChipIconStyle.Original
      else -> SnapshotDetailRuleChipIconStyle.Desaturated
    }
  )
}
