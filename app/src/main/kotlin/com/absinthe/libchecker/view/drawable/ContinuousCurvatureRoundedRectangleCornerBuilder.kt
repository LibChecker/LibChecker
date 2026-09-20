// Adapted from Kyant0/Shapes (Apache-2.0).
package com.absinthe.libchecker.view.drawable

import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal class ContinuousCurvatureRoundedRectangleCornerBuilder(
  val extendedFraction: Double = 2.0 / 3.0,
  val arcFraction: Double = 0.5
) {

  private val theta = (1.0 - arcFraction) * FRAC_PI_4
  private val cos = cos(theta)
  private val sin = sin(theta)
  private val cot = 1.0 / tan(theta)
  private val cos2 = cos * cos
  private val sin2 = sin * sin
  private val cos3 = cos2 * cos
  private val sin3 = sin2 * sin

  private val k0 =
    27.0 * (SQRT_2 - 6.0 * cos + 6.0 * SQRT_2 * cos2 - 4.0 * cos3) * cot +
      2.0 * sin * (-9.0 + 2.0 * (SQRT_2 - 2.0 * sin) * sin3 + 2.0 * SQRT_2 * cos * (9.0 + sin2) - 2.0 * cos2 * (9.0 + 2.0 * sin2))
  private val k1 =
    -81.0 * (-2.0 + SQRT_2 + 4.0 * (-1.0 + SQRT_2) * cos + 2.0 * (-2.0 + SQRT_2) * cos2) * cot -
      4.0 * sin * (-9.0 + 9.0 * SQRT_2 + SQRT_2 * sin3 + (-2.0 + SQRT_2) * cos * (9.0 + sin2))
  private val k2 =
    9.0 * (9.0 * (-4.0 + 3.0 * SQRT_2 + (-6.0 + 4.0 * SQRT_2) * cos) * cot + (-6.0 + 4.0 * SQRT_2) * sin)
  private val k3 =
    27.0 * (10.0 - 7.0 * SQRT_2) * cot

  private val cache =
    arrayOf(
      arrayOf(
        buildEvenCornerBezierPoints(0.0),
        buildUnevenCornerBezierPoints(0.0, 1.0)
      ),
      arrayOf(
        buildUnevenCornerBezierPoints(1.0, 0.0),
        buildEvenCornerBezierPoints(1.0)
      )
    )

  fun getCornerBezierPoints(tH: Double = 1.0, tV: Double = 1.0): DoubleArray {
    val i = when (tH) {
      0.0 -> 0
      1.0 -> 1
      else -> return buildCornerBezierPoints(tH, tV)
    }
    val j = when (tV) {
      0.0 -> 0
      1.0 -> 1
      else -> return buildCornerBezierPoints(tH, tV)
    }
    return cache[i][j]
  }

  private fun buildCornerBezierPoints(tH: Double = 1.0, tV: Double = 1.0): DoubleArray {
    return if (tH == tV) {
      buildEvenCornerBezierPoints(tH)
    } else {
      buildUnevenCornerBezierPoints(tH, tV)
    }
  }

  private fun buildEvenCornerBezierPoints(t: Double = 1.0): DoubleArray {
    val k = extendedFraction * t

    val kappa = solveCubicSingle(k3, k2, k1 + 8.0 * (-k) * sin3 * sin, k0)

    val x3 = FRAC_1_SQRT_2 + (-FRAC_1_SQRT_2 + sin) / kappa
    val y3 = 1.0 - FRAC_1_SQRT_2 + (FRAC_1_SQRT_2 - cos) / kappa
    val x2 = x3 - y3 * cot
    val x1 = x2 - 1.5 * kappa * y3 * y3 / sin3
    val x0 = -k

    val x6 = 1.0 - y3
    val y6 = 1.0 - x3
    val y7 = 1.0 - x2
    val y8 = 1.0 - x1
    val y9 = 1.0 - x0

    val a = 1.5 * kappa
    val g = cos2 - sin2
    val x36 = x6 - x3
    val y36 = y6 - y3
    val c = -(cos * y36 - sin * x36)
    val lambda = (-g + sqrt(g * g - 4.0 * a * c)) / (2.0 * a)
    val x4 = x3 + lambda * cos
    val y4 = y3 + lambda * sin
    val x5 = x6 - lambda * sin
    val y5 = y6 - lambda * cos

    return doubleArrayOf(x0, 0.0, x1, 0.0, x2, 0.0, x3, y3, x4, y4, x5, y5, x6, y6, 1.0, y7, 1.0, y8, 1.0, y9)
  }

  private fun buildUnevenCornerBezierPoints(tH: Double = 1.0, tV: Double = 1.0): DoubleArray {
    val kH = extendedFraction * tH
    val kV = extendedFraction * tV

    val kappa3 = solveCubicSingle(k3, k2, k1 + 8.0 * (-kH) * sin3 * sin, k0)
    val kappa6 = solveCubicSingle(k3, k2, k1 + 8.0 * (-kV) * sin3 * sin, k0)

    val x3 = FRAC_1_SQRT_2 + (-FRAC_1_SQRT_2 + sin) / kappa3
    val y3 = 1.0 - FRAC_1_SQRT_2 + (FRAC_1_SQRT_2 - cos) / kappa3
    val x2 = x3 - y3 * cot
    val x1 = x2 - 1.5 * kappa3 * y3 * y3 / sin3
    val x0 = -kH

    val x3p = FRAC_1_SQRT_2 + (-FRAC_1_SQRT_2 + sin) / kappa6
    val y3p = 1.0 - FRAC_1_SQRT_2 + (FRAC_1_SQRT_2 - cos) / kappa6
    val x2p = x3p - y3p * cot
    val x1p = x2p - 1.5 * kappa6 * y3p * y3p / sin3
    val x0p = -kV
    val x6 = 1.0 - y3p
    val y6 = 1.0 - x3p
    val y7 = 1.0 - x2p
    val y8 = 1.0 - x1p
    val y9 = 1.0 - x0p

    val a = 1.5 * kappa3
    val b = 1.5 * kappa6
    val g = cos2 - sin2
    val x36 = x6 - x3
    val y36 = y6 - y3
    val c = -(cos * y36 - sin * x36)
    val d = sin * y36 - cos * x36
    val p = 2.0 * (d / b)
    val q = g * g * g / (a * b * b)
    val r = (a * d * d + c * g * g) / (a * b * b)
    val lambda6 = solveDepressedQuarticSingle(p, q, r)
    val lambda3 = (-d - b * lambda6 * lambda6) / g
    val x4 = x3 + lambda3 * cos
    val y4 = y3 + lambda3 * sin
    val x5 = x6 - lambda6 * sin
    val y5 = y6 - lambda6 * cos

    return doubleArrayOf(x0, 0.0, x1, 0.0, x2, 0.0, x3, y3, x4, y4, x5, y5, x6, y6, 1.0, y7, 1.0, y8, 1.0, y9)
  }

  companion object {

    val Default = ContinuousCurvatureRoundedRectangleCornerBuilder()
  }
}

private fun solveCubicSingle(a: Double, b: Double, c: Double, d: Double): Double {
  val f = ((3.0 * c / a) - (b * b) / (a * a)) / 3.0
  val g = ((2.0 * b * b * b) / (a * a * a) - (9.0 * b * c) / (a * a) + (27.0 * d) / a) / 27.0
  val h = g * g / 4.0 + f * f * f / 27.0
  val sqrtH = sqrt(h)
  return cbrt(-g / 2.0 + sqrtH) + cbrt(-g / 2.0 - sqrtH) - b / (3.0 * a)
}

private fun solveDepressedQuarticSingle(p: Double, q: Double, r: Double): Double {
  val b = -p / 2.0
  val c = -r
  val d = r * p / 2.0 - q * q / 8.0
  val f = ((3.0 * c) - (b * b)) / 3.0
  val g = ((2.0 * b * b * b) - (9.0 * b * c) + (27.0 * d)) / 27.0
  val r = sqrt(-f * f * f / 27.0)
  val phi = acos(-g / (2.0 * r))
  val y = 2.0 * sqrt(-f / 3.0) * cos(phi / 3.0)
  val z = y - b / 3.0
  val u = sqrt(2.0 * z - p)
  return (u - sqrt(u * u - 4.0 * (z + q / (2.0 * u)))) / 2.0
}

private const val SQRT_2: Double = 1.4142135623730951
private const val FRAC_PI_4: Double = 0.7853981633974483
private const val FRAC_1_SQRT_2: Double = 0.7071067811865476
