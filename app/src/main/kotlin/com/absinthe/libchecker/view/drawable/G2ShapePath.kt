package com.absinthe.libchecker.view.drawable

import android.graphics.Path

/** Three curvature-matched cubics per corner, adapted from Kyant0/Shapes. */
fun Path.setG2Shape(
  left: Float,
  top: Float,
  right: Float,
  bottom: Float,
  cornerRadius: Float,
  tailCenter: Float? = null,
  tailWidth: Float = 0f,
  tailHeight: Float = 0f,
  cornerSmoothing: Float? = null,
  rightCornerRadius: Float = cornerRadius
) {
  rewind()
  if (right <= left || bottom <= top) return
  val r = cornerRadius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
  val rightRadius = rightCornerRadius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
  val smoothing = cornerSmoothing?.coerceIn(0f, 1f)
  if (smoothing == 0f && (tailCenter == null || tailWidth <= 0f || tailHeight <= 0f)) {
    if (r == rightRadius) {
      addRoundRect(left, top, right, bottom, r, r, Path.Direction.CW)
    } else {
      addRoundRect(left, top, right, bottom, floatArrayOf(r, r, rightRadius, rightRadius, rightRadius, rightRadius, r, r), Path.Direction.CW)
    }
    return
  }
  // Share horizontal space by radius so unequal corners keep a smooth join even on short bars.
  val radii = r.toDouble() + rightRadius
  val horizontal = if (radii == 0.0) 1.0 else (((right - left) - radii) / radii).coerceIn(0.0, 1.0)
  val leftPoints = cornerPoints(horizontal, bottom - top, r, smoothing ?: 1f)
  val rightPoints = if (r == rightRadius) leftPoints else cornerPoints(horizontal, bottom - top, rightRadius, smoothing ?: 1f)
  val leftExtent = r * (1f - leftPoints[0].toFloat())
  val rightExtent = rightRadius * (1f - rightPoints[0].toFloat())
  moveTo(left + leftExtent, top)
  addG2Side(right, top, rightRadius, bottom - top, rightPoints, 1f)

  if (tailCenter != null && tailWidth > 0f && tailHeight > 0f) {
    val w = minOf(tailWidth / 2f, (right - left - leftExtent - rightExtent) / 2f)
    val c = tailCenter.coerceIn(left + leftExtent + w, right - rightExtent - w)
    val h = tailHeight
    // Rounded shoulders and a soft tip share the body's outline; no attached triangle.
    lineTo(c + w, bottom)
    cubicTo(c + w * .65f, bottom, c + w * .58f, bottom, c + w * .40f, bottom + h * .44f)
    cubicTo(c + w * .22f, bottom + h * .88f, c + w * .14f, bottom + h, c, bottom + h)
    cubicTo(c - w * .14f, bottom + h, c - w * .22f, bottom + h * .88f, c - w * .40f, bottom + h * .44f)
    cubicTo(c - w * .58f, bottom, c - w * .65f, bottom, c - w, bottom)
  }

  addG2Side(left, bottom, r, bottom - top, leftPoints, -1f)
  close()
}

private fun cornerPoints(horizontal: Double, height: Float, radius: Float, smoothing: Float): DoubleArray {
  val vertical = if (radius == 0f) 1.0 else ((height * 0.5 - radius) / radius).coerceIn(0.0, 1.0)
  return ContinuousCurvatureRoundedRectangleCornerBuilder.Default.getCornerBezierPoints(horizontal * smoothing, vertical * smoothing)
}

private fun Path.addG2Side(x: Float, y: Float, r: Float, height: Float, p: DoubleArray, direction: Float) {
  if (r == 0f) {
    lineTo(x, y)
    lineTo(x, y + direction * height)
    return
  }
  val originX = x - direction * r
  val scale = direction * r
  lineTo(originX + scale * p[0].toFloat(), y)
  for (i in 2..14 step 6) {
    cubicTo(
      originX + scale * p[i].toFloat(),
      y + scale * p[i + 1].toFloat(),
      originX + scale * p[i + 2].toFloat(),
      y + scale * p[i + 3].toFloat(),
      originX + scale * p[i + 4].toFloat(),
      y + scale * p[i + 5].toFloat()
    )
  }
  val bottom = y + direction * height
  lineTo(originX + scale * p[18].toFloat(), bottom - scale * p[19].toFloat())
  for (i in 16 downTo 4 step 6) {
    cubicTo(
      originX + scale * p[i].toFloat(),
      bottom - scale * p[i + 1].toFloat(),
      originX + scale * p[i - 2].toFloat(),
      bottom - scale * p[i - 1].toFloat(),
      originX + scale * p[i - 4].toFloat(),
      bottom - scale * p[i - 3].toFloat()
    )
  }
}
