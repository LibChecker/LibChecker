package com.absinthe.libchecker.domain.statistics.reference.ui.view

import kotlin.math.max
import kotlin.math.min

internal object TreemapLayout {
  data class Entry(val id: String, val weight: Double, val sortLast: Boolean = false)
  data class Tile(val id: String, val left: Double, val top: Double, val right: Double, val bottom: Double)

  fun layout(entries: List<Entry>, width: Double, height: Double): List<Tile> {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return emptyList()
    val sorted = entries.filter { it.weight.isFinite() && it.weight > 0 }
      .sortedWith(compareBy<Entry> { it.sortLast }.thenByDescending { it.weight }.thenBy { it.id })
    val maximum = sorted.maxOfOrNull { it.weight } ?: return emptyList()
    val total = sorted.sumOf { it.weight / maximum }
    val areas = sorted.map { it.weight / maximum / total * width * height }
    val result = ArrayList<Tile>(sorted.size)
    var left = 0.0
    var top = 0.0
    var remainingWidth = width
    var remainingHeight = height
    var start = 0
    while (start < sorted.size && remainingWidth > 0 && remainingHeight > 0) {
      val side = min(remainingWidth, remainingHeight)
      var end = start + 1
      var sum = areas[start]
      var largest = areas[start]
      var smallest = areas[start]
      var worst = aspect(sum, largest, smallest, side)
      while (end < sorted.size) {
        val nextSum = sum + areas[end]
        val nextLargest = max(largest, areas[end])
        val nextSmallest = min(smallest, areas[end])
        val nextWorst = aspect(nextSum, nextLargest, nextSmallest, side)
        if (nextWorst > worst) break
        sum = nextSum
        largest = nextLargest
        smallest = nextSmallest
        worst = nextWorst
        end++
      }
      val vertical = remainingWidth >= remainingHeight
      val thickness = min(sum / side, if (vertical) remainingWidth else remainingHeight)
      var offset = if (vertical) top else left
      for (index in start until end) {
        val edge = if (index == end - 1) {
          if (vertical) top + remainingHeight else left + remainingWidth
        } else {
          offset + areas[index] / thickness
        }
        result += if (vertical) {
          Tile(sorted[index].id, left, offset, left + thickness, edge)
        } else {
          Tile(sorted[index].id, offset, top, edge, top + thickness)
        }
        offset = edge
      }
      if (vertical) {
        left += thickness
        remainingWidth = (width - left).coerceAtLeast(0.0)
      } else {
        top += thickness
        remainingHeight = (height - top).coerceAtLeast(0.0)
      }
      start = end
    }
    return result
  }

  private fun aspect(sum: Double, largest: Double, smallest: Double, side: Double): Double {
    val square = sum * sum
    val sideSquare = side * side
    return max(sideSquare * largest / square, square / (sideSquare * smallest))
  }
}
