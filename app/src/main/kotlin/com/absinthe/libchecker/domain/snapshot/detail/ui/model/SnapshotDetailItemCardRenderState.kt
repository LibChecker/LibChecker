package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class SnapshotDetailItemViewRenderState(
  val title: CharSequence,
  val extra: CharSequence,
  @DrawableRes val iconRes: Int,
  @ColorRes val statusColorRes: Int,
  @StringRes val statusLabelRes: Int,
  val contentDescription: String,
  val ruleChip: SnapshotDetailRuleChipRenderState?
)

data class SnapshotDetailThemeColors(
  @ColorInt val surface: Int,
  @ColorInt val onSurface: Int,
  @ColorInt val onSurfaceVariant: Int,
  @ColorInt val outlineVariant: Int,
  @ColorInt val chipSurface: Int
)

data class SnapshotDetailItemResolvedColors(
  @ColorInt val surface: Int,
  @ColorInt val title: Int,
  @ColorInt val supportingText: Int,
  @ColorInt val divider: Int,
  @ColorInt val status: Int,
  @ColorInt val chipSurface: Int,
  @ColorInt val chipText: Int,
  @ColorInt val chipOutline: Int
)

fun resolveSnapshotDetailItemColors(
  theme: SnapshotDetailThemeColors,
  @ColorInt statusColor: Int
): SnapshotDetailItemResolvedColors {
  return SnapshotDetailItemResolvedColors(
    surface = theme.surface,
    title = theme.onSurface,
    supportingText = theme.onSurfaceVariant,
    divider = theme.outlineVariant,
    status = statusColor,
    chipSurface = theme.chipSurface,
    chipText = theme.onSurfaceVariant,
    chipOutline = theme.outlineVariant
  )
}

data class SnapshotDetailItemLayoutPlan(
  val titleStartsOnNewLine: Boolean,
  val titleWidth: Int,
  val chipOnTitleLine: Boolean
)

fun planSnapshotDetailItemLayout(
  contentWidth: Int,
  statusClusterWidth: Int,
  naturalTitleWidth: Int,
  chipWidth: Int,
  titleGap: Int,
  chipGap: Int,
  minimumTitleWidth: Int
): SnapshotDetailItemLayoutPlan {
  val safeContentWidth = contentWidth.coerceAtLeast(0)
  val inlineTitleWidth = (safeContentWidth - statusClusterWidth - titleGap).coerceAtLeast(0)
  val titleStartsOnNewLine = inlineTitleWidth < minimumTitleWidth
  val titleWidth = if (titleStartsOnNewLine) safeContentWidth else inlineTitleWidth
  val titleWraps = naturalTitleWidth > titleWidth
  val chipOnTitleLine = chipWidth > 0 &&
    !titleWraps &&
    naturalTitleWidth + chipGap + chipWidth <= titleWidth
  return SnapshotDetailItemLayoutPlan(
    titleStartsOnNewLine = titleStartsOnNewLine,
    titleWidth = titleWidth,
    chipOnTitleLine = chipOnTitleLine
  )
}
