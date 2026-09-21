package com.absinthe.libchecker.view.drawable

import com.google.android.material.shape.CornerTreatment
import com.google.android.material.shape.ShapePath

/** Material adapter for the same three-cubic corners used by [setG2Shape]. */
internal class G2CornerTreatment : CornerTreatment() {
  override fun getCornerPath(shapePath: ShapePath, angle: Float, interpolation: Float, radius: Float) {
    val r = radius * interpolation
    val points = ContinuousCurvatureRoundedRectangleCornerBuilder.Default.getCornerBezierPoints()
    shapePath.reset(0f, r * (1f - points[0].toFloat()))
    for (i in 2..14 step 6) {
      shapePath.cubicToPoint(
        r * points[i + 1].toFloat(),
        r * (1f - points[i].toFloat()),
        r * points[i + 3].toFloat(),
        r * (1f - points[i + 2].toFloat()),
        r * points[i + 5].toFloat(),
        r * (1f - points[i + 4].toFloat())
      )
    }
  }
}
