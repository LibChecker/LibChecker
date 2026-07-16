package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipIconStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipRenderState

data class SnapshotDetailItemRenderState(
  val title: CharSequence,
  val extra: CharSequence,
  @DrawableRes val iconRes: Int,
  @ColorRes val statusColorRes: Int,
  @StringRes val statusLabelRes: Int,
  val contentDescription: String,
  val ruleChip: SnapshotDetailRuleChipRenderState?,
  val chipClickAction: SnapshotDetailNodeChipClickAction?
)

val BaseSnapshotNode.itemRenderState: SnapshotDetailItemRenderState
  get() = SnapshotDetailItemRenderState(
    title = displayData.title,
    extra = displayData.extra,
    iconRes = displayData.status.iconRes,
    statusColorRes = displayData.status.colorRes,
    statusLabelRes = displayData.status.labelRes,
    contentDescription = displayData.description,
    ruleChip = displayData.ruleChip?.toRenderState(),
    chipClickAction = chipClickAction
  )

val SnapshotDetailItemRenderState.viewRenderState: SnapshotDetailItemViewRenderState
  get() = SnapshotDetailItemViewRenderState(
    title = title,
    extra = extra,
    iconRes = iconRes,
    statusColorRes = statusColorRes,
    statusLabelRes = statusLabelRes,
    contentDescription = contentDescription,
    ruleChip = ruleChip
  )

private fun SnapshotDetailRuleChipDisplayData.toRenderState(): SnapshotDetailRuleChipRenderState {
  return SnapshotDetailRuleChipRenderState(
    label = label,
    iconRes = iconRes,
    iconStyle = when {
      isSimpleColorIcon -> SnapshotDetailRuleChipIconStyle.ThemeTint
      useColorfulIcon -> SnapshotDetailRuleChipIconStyle.Original
      else -> SnapshotDetailRuleChipIconStyle.Desaturated
    }
  )
}
