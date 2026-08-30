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
  tailHeight: Float = 0f
) {
  rewind()
  if (right <= left || bottom <= top) return
  val r = cornerRadius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
  val a = r * 0.05893696f
  val b = r * 0.41421357f
  val d = r * 0.70710677f
  val inset = r - d
  // A pill joins its two curved halves directly. A rounded rectangle also needs zero
  // curvature at the vertical edges, so mirror the first half across the diagonal.
  val hasStraightSides = bottom - top > 2f * r
  val x = if (hasStraightSides) r else r * 0.92621285f
  val y = if (hasStraightSides) r - b else r * 0.5119993f
  val side = if (hasStraightSides) r - a else r * 0.7845774f

  moveTo(left + r, top)
  lineTo(right - r, top)
  cubicTo(right - r + a, top, right - r + b, top, right - r + d, top + inset)
  cubicTo(right - r + x, top + y, right, top + side, right, top + r)
  lineTo(right, bottom - r)
  cubicTo(right, bottom - side, right - r + x, bottom - y, right - r + d, bottom - inset)
  cubicTo(right - r + b, bottom, right - r + a, bottom, right - r, bottom)

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
  cubicTo(left + r - a, bottom, left + r - b, bottom, left + inset, bottom - inset)
  cubicTo(left + r - x, bottom - y, left, bottom - side, left, bottom - r)
  lineTo(left, top + r)
  cubicTo(left, top + side, left + r - x, top + y, left + inset, top + inset)
  cubicTo(left + r - b, top, left + r - a, top, left + r, top)
  close()
}
