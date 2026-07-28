package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailMovedPathRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipIconStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipRenderState
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem

data class SnapshotDetailItemRenderState(
  val title: CharSequence,
  val extra: CharSequence,
  @DrawableRes val iconRes: Int,
  @ColorRes val statusColorRes: Int,
  @StringRes val statusLabelRes: Int,
  val contentDescription: String,
  val ruleChip: SnapshotDetailRuleChipRenderState?,
  val chipClickAction: SnapshotDetailNodeChipClickAction?,
  val movedPath: SnapshotDetailMovedPathRenderState? = null
)

val BaseSnapshotNode.itemRenderState: SnapshotDetailItemRenderState
  get() {
    val movedPath = item.toMovedPathRenderState()
    return SnapshotDetailItemRenderState(
      title = if (movedPath == null) displayData.title else item.name,
      extra = displayData.extra,
      iconRes = displayData.status.iconRes,
      statusColorRes = displayData.status.colorRes,
      statusLabelRes = displayData.status.labelRes,
      contentDescription = displayData.description,
      ruleChip = displayData.ruleChip?.toRenderState(),
      chipClickAction = chipClickAction,
      movedPath = movedPath
    )
  }

val SnapshotDetailItemRenderState.viewRenderState: SnapshotDetailItemViewRenderState
  get() = SnapshotDetailItemViewRenderState(
    title = title,
    extra = extra,
    iconRes = iconRes,
    statusColorRes = statusColorRes,
    statusLabelRes = statusLabelRes,
    contentDescription = contentDescription,
    ruleChip = ruleChip,
    movedPath = movedPath
  )

private fun SnapshotDetailItem.toMovedPathRenderState(): SnapshotDetailMovedPathRenderState? {
  if (diffType != MOVED) return null
  val previousPackagePath = previousName
    ?.substringBeforeLast('.', missingDelimiterValue = "")
    ?.takeIf(String::isNotBlank)
    ?: return null
  return SnapshotDetailMovedPathRenderState(previousPackagePath)
}

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
