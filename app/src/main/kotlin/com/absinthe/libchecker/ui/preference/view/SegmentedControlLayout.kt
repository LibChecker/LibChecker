package com.absinthe.libchecker.ui.preference.view

import kotlin.math.roundToInt

internal fun calculateSegmentWidth(
  preferredWidth: Int,
  segmentCount: Int,
  groupInset: Int,
  availableWidth: Int
): Int {
  require(preferredWidth > 0)
  require(segmentCount > 0)
  require(groupInset >= 0)

  val availableContentWidth = (availableWidth - groupInset * 2).coerceAtLeast(segmentCount)
  return preferredWidth.coerceAtMost(availableContentWidth / segmentCount)
}

internal fun segmentTranslation(
  index: Int,
  segmentWidth: Int,
  isRtl: Boolean
): Float {
  val direction = if (isRtl) -1 else 1
  return direction * index * segmentWidth.toFloat()
}

internal fun segmentIndexForPointer(
  pointerX: Float,
  viewWidth: Int,
  segmentWidth: Int,
  segmentCount: Int,
  groupInset: Int,
  isRtl: Boolean
): Int {
  val logicalTranslation = logicalSegmentTranslationForPointer(
    pointerX = pointerX,
    viewWidth = viewWidth,
    segmentWidth = segmentWidth,
    segmentCount = segmentCount,
    groupInset = groupInset,
    isRtl = isRtl
  )
  return (logicalTranslation / segmentWidth).roundToInt().coerceIn(0, segmentCount - 1)
}

internal fun logicalSegmentTranslationForPointer(
  pointerX: Float,
  viewWidth: Int,
  segmentWidth: Int,
  segmentCount: Int,
  groupInset: Int,
  isRtl: Boolean
): Float {
  val pointerFromStart = if (isRtl) viewWidth - pointerX else pointerX
  val maxLogicalTranslation = ((segmentCount - 1) * segmentWidth).toFloat()
  return (
    pointerFromStart -
      groupInset -
      segmentWidth / 2f
    ).coerceIn(0f, maxLogicalTranslation)
}

internal fun shouldNotifySegmentSelection(
  selectedIndex: Int,
  requestedIndex: Int,
  hasValidSelectedValue: Boolean
): Boolean {
  return !hasValidSelectedValue || selectedIndex != requestedIndex
}
