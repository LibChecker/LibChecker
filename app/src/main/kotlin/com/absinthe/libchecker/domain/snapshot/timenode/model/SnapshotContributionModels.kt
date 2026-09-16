package com.absinthe.libchecker.domain.snapshot.timenode.model

import android.graphics.Color
import com.absinthe.libraries.utils.utils.UiUtils
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object SnapshotPalette {

  /**
   * Precomputed OKLCH perceptually uniform palette (12 golden-ratio hues).
   * In Light Mode: L = 0.62, C = 0.14
   * In Dark Mode:  L = 0.74, C = 0.13
   */
  val LIGHT_COLORS = intArrayOf(
    0xFFCD605A.toInt(), // Coral (Hue 25.0°)
    0xFF009F6C.toInt(), // Emerald (Hue 162.5°)
    0xFF9470CD.toInt(), // Purple (Hue 300.0°)
    0xFFB47900.toInt(), // Amber (Hue 77.5°)
    0xFF0099BA.toInt(), // Cyan-Teal (Hue 215.0°)
    0xFFC45E8E.toInt(), // Rose (Hue 352.5°)
    0xFF699630.toInt(), // Olive Green (Hue 130.0°)
    0xFF6181DA.toInt(), // Royal Blue (Hue 267.6°)
    0xFFC96736.toInt(), // Tangerine (Hue 45.1°)
    0xFF00A08D.toInt(), // Mint-Teal (Hue 182.6°)
    0xFFAB67BA.toInt(), // Magenta-Violet (Hue 320.1°)
    0xFF9E8500.toInt() // Gold (Hue 97.6°)
  )

  val DARK_COLORS = intArrayOf(
    0xFFF28881.toInt(), // Coral
    0xFF4DC492.toInt(), // Emerald
    0xFFB897F0.toInt(), // Lilac Purple
    0xFFD89F3E.toInt(), // Soft Amber
    0xFF07BFDE.toInt(), // Cyan-Sky
    0xFFE987B3.toInt(), // Pastel Rose
    0xFF8FBB5F.toInt(), // Light Olive
    0xFF87A7FD.toInt(), // Periwinkle Blue
    0xFFEE8F63.toInt(), // Peach
    0xFF12C5B2.toInt(), // Mint
    0xFFCF8FDE.toInt(), // Violet
    0xFFC1AB3C.toInt() // Sand Gold
  )

  val COLORS = LIGHT_COLORS

  fun getColor(index: Int, isDarkMode: Boolean = isSystemDarkMode()): Int {
    val palette = if (isDarkMode) DARK_COLORS else LIGHT_COLORS
    val positiveIndex = Math.floorMod(index, palette.size)
    return palette[positiveIndex]
  }

  fun oklchToColor(l: Double, c: Double, h: Double): Int {
    val hRad = Math.toRadians(h)
    val a = c * cos(hRad)
    val b = c * sin(hRad)

    val lCone = l + 0.3963377774 * a + 0.2158037573 * b
    val mCone = l - 0.1055613458 * a - 0.0638541728 * b
    val sCone = l - 0.0894841775 * a - 1.2914855480 * b

    val l3 = lCone * lCone * lCone
    val m3 = mCone * mCone * mCone
    val s3 = sCone * sCone * sCone

    val rLinear = +4.076743529 * l3 - 3.307711591 * m3 + 0.230969929 * s3
    val gLinear = -1.268438004 * l3 + 2.609757401 * m3 - 0.341319396 * s3
    val bLinear = -0.004196086 * l3 - 0.703418614 * m3 + 1.707614701 * s3

    fun toSrgb(channel: Double): Int {
      val clamped = channel.coerceIn(0.0, 1.0)
      val srgb = if (clamped <= 0.0031308) {
        12.92 * clamped
      } else {
        1.055 * clamped.pow(1.0 / 2.4) - 0.055
      }
      return (srgb * 255.0).roundToInt().coerceIn(0, 255)
    }

    val red = toSrgb(rLinear)
    val green = toSrgb(gLinear)
    val blue = toSrgb(bLinear)
    return Color.rgb(red, green, blue)
  }

  private fun isSystemDarkMode(): Boolean = runCatching { UiUtils.isDarkMode() }.getOrDefault(false)
}

data class SnapshotUpdatedAppItem(
  val packageName: String,
  val label: String,
  val previousSnapshotTimestamp: Long? = null,
  val currentSnapshotTimestamp: Long? = null
)

data class DayContribution(
  val date: LocalDate,
  val updateCount: Int = 0,
  val snapshotTimestamp: Long? = null,
  val snapshotColor: Int? = null,
  val isSnapshotDay: Boolean = false,
  val updatedApps: List<SnapshotUpdatedAppItem> = emptyList(),
  val previousSnapshotTimestamp: Long? = null,
  val currentSnapshotTimestamp: Long? = null
)

data class SnapshotContributionData(
  val days: Map<LocalDate, DayContribution>,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val snapshotColors: Map<Long, Int>,
  val totalUpdates: Int
)
