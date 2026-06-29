package com.google.android.material.shape

class SuperEllipseCornerTreatment(val r: Float) : RoundedCornerTreatment() {

  init {
    radius = r
  }

  private val n: Float = 3.5f
  private val exponent: Float = if (n <= 1f) 2f else n
  private val kNorm: Float = 0.5f * (1f + 2f / exponent)
  private val c1xNorm = 0f
  private val c1yNorm = kNorm
  private val c2xNorm = 1f - kNorm
  private val c2yNorm = 0f

  override fun getCornerPath(
    shapePath: ShapePath,
    angle: Float,
    interpolation: Float,
    radius: Float
  ) {
    val currentR = radius * interpolation

    if (currentR <= 0f) {
      shapePath.reset(0f, 0f)
      return
    }

    val c1x = c1xNorm * currentR
    val c1y = c1yNorm * currentR
    val c2x = c2xNorm * currentR
    val c2y = c2yNorm * currentR

    shapePath.reset(0f, currentR)

    shapePath.cubicToPoint(
      c1x,
      c1y,
      c2x,
      c2y,
      currentR,
      0f
    )
  }
}
