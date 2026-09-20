package com.absinthe.libchecker.view.drawable

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class G2CornerContinuityTest {
  @Test
  fun joinsKeepTangentAndCurvatureAcrossAvailableSpace() {
    val builder = ContinuousCurvatureRoundedRectangleCornerBuilder.Default
    for (horizontal in 0..20) {
      for (vertical in 0..20) {
        val p = builder.getCornerBezierPoints(horizontal / 20.0, vertical / 20.0)
        assertTrue(p.all { it.isFinite() })
        for (join in listOf(6, 12)) {
          val before = derivatives(p, join - 6, true)
          val after = derivatives(p, join, false)
          val beforeSpeed = hypot(before[0], before[1])
          val afterSpeed = hypot(after[0], after[1])
          assertEquals(before[0] / beforeSpeed, after[0] / afterSpeed, 1e-7)
          assertEquals(before[1] / beforeSpeed, after[1] / afterSpeed, 1e-7)
          assertEquals(curvature(before), curvature(after), 1e-7)
        }
        for (start in listOf(0, 6, 12)) {
          for (sample in 1..19) {
            val t = sample / 20.0
            val u = 1 - t
            val d = DoubleArray(4)
            for (axis in 0..1) {
              val a = p[start + axis]
              val b = p[start + 2 + axis]
              val c = p[start + 4 + axis]
              val e = p[start + 6 + axis]
              d[axis] = 3 * (u * u * (b - a) + 2 * u * t * (c - b) + t * t * (e - c))
              d[axis + 2] = 6 * (u * (c - 2 * b + a) + t * (e - 2 * c + b))
            }
            assertTrue("Corner must not bend back", curvature(d) >= -1e-7)
          }
        }
        if (horizontal > 0) assertEquals(0.0, curvature(derivatives(p, 0, false)), 1e-7)
        if (vertical > 0) assertEquals(0.0, curvature(derivatives(p, 12, true)), 1e-7)
      }
    }
  }

  @Test
  fun controlsChangeContinuouslyAtCapsuleAndFullSmoothingLimits() {
    val builder = ContinuousCurvatureRoundedRectangleCornerBuilder.Default
    for (edge in listOf(0.0, 1.0)) {
      val near = if (edge == 0.0) 1e-6 else 1.0 - 1e-6
      for (other in listOf(0.0, 0.3, 1.0)) {
        val exact = builder.getCornerBezierPoints(other, edge)
        val adjacent = builder.getCornerBezierPoints(other, near)
        assertTrue(exact.indices.all { abs(exact[it] - adjacent[it]) < 1e-4 })
      }
    }
  }

  private fun derivatives(p: DoubleArray, start: Int, end: Boolean): DoubleArray {
    val i = if (end) start + 6 else start
    val direction = if (end) -2 else 2
    val dx = 3 * (p[i + direction] - p[i]) * if (end) -1 else 1
    val dy = 3 * (p[i + direction + 1] - p[i + 1]) * if (end) -1 else 1
    val ax = 6 * (p[i] - 2 * p[i + direction] + p[i + 2 * direction])
    val ay = 6 * (p[i + 1] - 2 * p[i + direction + 1] + p[i + 2 * direction + 1])
    return doubleArrayOf(dx, dy, ax, ay)
  }

  private fun curvature(d: DoubleArray): Double {
    val speed = hypot(d[0], d[1])
    return (d[0] * d[3] - d[1] * d[2]) / (speed * speed * speed)
  }
}
