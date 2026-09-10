package com.absinthe.libchecker.domain.statistics.reference.ui.view

internal object TreemapIconColor {
  /** Returns no accent for transparent, single-color or grayscale artwork. */
  fun dominantColor(pixels: IntArray): Int? {
    val histogram = mutableMapOf<Int, Int>()
    pixels.forEach { pixel ->
      if (pixel ushr 24 >= 128) {
        val key = (((pixel ushr 20) and 15) shl 8) or
          (((pixel ushr 12) and 15) shl 4) or ((pixel ushr 4) and 15)
        histogram[key] = (histogram[key] ?: 0) + 1
      }
    }
    val total = histogram.values.sum()
    val significant = histogram.filterValues { it >= maxOf(1, total / 20) }
    if (significant.size <= 1) return null
    fun channels(key: Int): List<Int> = listOf((key ushr 8) and 15, (key ushr 4) and 15, key and 15)
    if (significant.keys.all { key -> channels(key).let { it.max() - it.min() <= 1 } }) return null
    val key = histogram.maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })?.key ?: return null
    val red = ((key ushr 8) and 15) * 17
    val green = ((key ushr 4) and 15) * 17
    val blue = (key and 15) * 17
    return (255 shl 24) or (red shl 16) or (green shl 8) or blue
  }
}
