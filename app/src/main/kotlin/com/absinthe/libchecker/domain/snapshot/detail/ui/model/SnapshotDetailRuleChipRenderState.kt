package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class SnapshotDetailRuleChipRenderState(
  val label: String,
  @DrawableRes val iconRes: Int,
  @ColorInt val backgroundColor: Int,
  val iconStyle: SnapshotDetailRuleChipIconStyle
)

sealed interface SnapshotDetailRuleChipIconStyle {
  data object Original : SnapshotDetailRuleChipIconStyle
  data object Desaturated : SnapshotDetailRuleChipIconStyle
  data object BlackTint : SnapshotDetailRuleChipIconStyle
}
