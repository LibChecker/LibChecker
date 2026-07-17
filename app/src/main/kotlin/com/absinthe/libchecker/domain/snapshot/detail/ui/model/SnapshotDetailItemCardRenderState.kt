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
  val ruleChip: SnapshotDetailRuleChipRenderState?,
  val movedPath: SnapshotDetailMovedPathRenderState? = null
)

data class SnapshotDetailMovedPathRenderState(
  val previousPackagePath: CharSequence
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
  @ColorInt val gradientStart: Int,
  @ColorInt val gradientMiddle: Int,
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
    gradientStart = blendArgb(theme.surface, statusColor, overlayPercent = 10),
    gradientMiddle = blendArgb(theme.surface, statusColor, overlayPercent = 4),
    title = theme.onSurface,
    supportingText = theme.onSurfaceVariant,
    divider = theme.outlineVariant,
    status = statusColor,
    chipSurface = blendArgb(theme.chipSurface, statusColor, overlayPercent = 12),
    chipText = theme.onSurfaceVariant,
    chipOutline = theme.outlineVariant
  )
}

@ColorInt
private fun blendArgb(
  @ColorInt baseColor: Int,
  @ColorInt overlayColor: Int,
  overlayPercent: Int
): Int {
  val safeOverlayPercent = overlayPercent.coerceIn(0, 100)
  val basePercent = 100 - safeOverlayPercent

  fun blendComponent(shift: Int): Int {
    val baseComponent = baseColor ushr shift and 0xFF
    val overlayComponent = overlayColor ushr shift and 0xFF
    return (baseComponent * basePercent + overlayComponent * safeOverlayPercent) / 100
  }

  return blendComponent(24) shl 24 or
    (blendComponent(16) shl 16) or
    (blendComponent(8) shl 8) or
    blendComponent(0)
}

data class SnapshotDetailItemLayoutPlan(
  val titleWidth: Int,
  val chipOnTitleLine: Boolean
)

fun planSnapshotDetailItemLayout(
  contentWidth: Int,
  naturalTitleWidth: Int,
  chipWidth: Int,
  chipGap: Int
): SnapshotDetailItemLayoutPlan {
  val safeContentWidth = contentWidth.coerceAtLeast(0)
  val titleWraps = naturalTitleWidth > safeContentWidth
  val chipOnTitleLine = chipWidth > 0 &&
    !titleWraps &&
    naturalTitleWidth + chipGap + chipWidth <= safeContentWidth
  return SnapshotDetailItemLayoutPlan(
    titleWidth = safeContentWidth,
    chipOnTitleLine = chipOnTitleLine
  )
}
