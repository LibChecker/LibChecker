package com.absinthe.libchecker.view.drawable

import android.graphics.Path

/** Two cubic segments per corner, with curvature easing to zero at the straight edges. */
fun Path.setG2Shape(
  left: Float,
  top: Float,
  right: Float,
  bottom: Float,
  cornerRadius: Float,
  tailCenter: Float? = null,
  tailWidth: Float = 0f,
  tailHeight: Float = 0f,
  cornerSmoothing: Float? = null
) {
  rewind()
  if (right <= left || bottom <= top) return
  val r = cornerRadius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
  val smoothing = cornerSmoothing?.coerceIn(0f, 1f)
  val firstControl = r * if (smoothing == null) 0.05893696f else 0.0847f + 0.0215f * smoothing
  val secondControl = r * if (smoothing == null) 0.41421357f else 0.4377467f + 0.0217334f * smoothing
  val diagonal = r * if (smoothing == null) 0.70710677f else 0.7188733f + 0.0108668f * smoothing
  val diagonalInset = r - diagonal
  val hasStraightSides = bottom - top > 2f * r
  val outerControlX = if (smoothing == null) {
    if (hasStraightSides) r else r * 0.92621285f
  } else {
    r
  }
  val outerControlY = if (smoothing == null) {
    if (hasStraightSides) r - secondControl else r * 0.5119993f
  } else {
    diagonalInset * 2f
  }
  val sideControl = if (smoothing == null) {
    if (hasStraightSides) r - firstControl else r * 0.7845774f
  } else {
    r - firstControl
  }

  moveTo(left + r, top)
  lineTo(right - r, top)
  cubicTo(
    right - r + firstControl,
    top,
    right - r + secondControl,
    top,
    right - r + diagonal,
    top + diagonalInset
  )
  cubicTo(right - r + outerControlX, top + outerControlY, right, top + sideControl, right, top + r)
  lineTo(right, bottom - r)
  cubicTo(
    right,
    bottom - sideControl,
    right - r + outerControlX,
    bottom - outerControlY,
    right - r + diagonal,
    bottom - diagonalInset
  )
  cubicTo(
    right - r + secondControl,
    bottom,
    right - r + firstControl,
    bottom,
    right - r,
    bottom
  )

  if (tailCenter != null && tailWidth > 0f && tailHeight > 0f) {
    val w = minOf(tailWidth / 2f, (right - left - 2f * r) / 2f)
    val c = tailCenter.coerceIn(left + r + w, right - r - w)
    val h = tailHeight
    // Rounded shoulders and a soft tip share the body's outline; no attached triangle.
    lineTo(c + w, bottom)
    cubicTo(c + w * .65f, bottom, c + w * .58f, bottom, c + w * .40f, bottom + h * .44f)
    cubicTo(c + w * .22f, bottom + h * .88f, c + w * .14f, bottom + h, c, bottom + h)
    cubicTo(c - w * .14f, bottom + h, c - w * .22f, bottom + h * .88f, c - w * .40f, bottom + h * .44f)
    cubicTo(c - w * .58f, bottom, c - w * .65f, bottom, c - w, bottom)
  }

  lineTo(left + r, bottom)
  cubicTo(
    left + r - firstControl,
    bottom,
    left + r - secondControl,
    bottom,
    left + diagonalInset,
    bottom - diagonalInset
  )
  cubicTo(left + r - outerControlX, bottom - outerControlY, left, bottom - sideControl, left, bottom - r)
  lineTo(left, top + r)
  cubicTo(
    left,
    top + sideControl,
    left + r - outerControlX,
    top + outerControlY,
    left + diagonalInset,
    top + diagonalInset
  )
  cubicTo(
    left + r - secondControl,
    top,
    left + r - firstControl,
    top,
    left + r,
    top
  )
  close()
}
