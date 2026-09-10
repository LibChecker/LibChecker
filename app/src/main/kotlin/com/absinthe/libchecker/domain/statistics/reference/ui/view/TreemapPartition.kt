package com.absinthe.libchecker.domain.statistics.reference.ui.view

import kotlin.math.abs

/** Keeps split directions and membership fixed while weights change during a gesture. */
internal class TreemapPartition(entries: List<TreemapLayout.Entry>, width: Double, height: Double) {
  private class Node(val id: String?, val weight: Double, val vertical: Boolean, val first: Node? = null, val second: Node? = null)

  private fun build(tiles: List<TreemapLayout.Tile>, weights: Map<String, Double>): Node {
    if (tiles.size == 1) return Node(tiles[0].id, weights.getValue(tiles[0].id), false)
    val right = tiles.maxOf { it.right }
    val bottom = tiles.maxOf { it.bottom }
    var prefixRight = tiles[0].right
    var prefixBottom = tiles[0].bottom
    // Squarified rows form full cuts; retain those cuts while gesture weights change.
    for (split in 1 until tiles.size) {
      val remaining = tiles.subList(split, tiles.size)
      val vertical = abs(prefixBottom - bottom) < 0.000001 && remaining.all { it.left >= prefixRight - 0.000001 }
      val horizontal = abs(prefixRight - right) < 0.000001 && remaining.all { it.top >= prefixBottom - 0.000001 }
      if (vertical || horizontal) {
        val first = build(tiles.subList(0, split), weights)
        val second = build(remaining, weights)
        return Node(null, first.weight + second.weight, vertical, first, second)
      }
      prefixRight = maxOf(prefixRight, tiles[split].right)
      prefixBottom = maxOf(prefixBottom, tiles[split].bottom)
    }
    error("Squarified layout must have a full row cut")
  }

  private val root = TreemapLayout.layout(entries, width, height)
    .takeIf { it.isNotEmpty() }
    ?.let { build(it, entries.associate { entry -> entry.id to entry.weight }) }

  fun layout(width: Double, height: Double, from: Set<String>?, to: Set<String>?, progress: Float): List<TreemapLayout.Tile> {
    if (width <= 0 || height <= 0) return emptyList()
    val weights = HashMap<Node, Double>()
    val p = progress.coerceIn(0f, 1f).toDouble()
    fun weigh(node: Node): Double {
      val weight = if (node.id != null) {
        node.weight * ((if (from == null || node.id in from) 1 - p else 0.0) + (if (to == null || node.id in to) p else 0.0))
      } else {
        weigh(checkNotNull(node.first)) + weigh(checkNotNull(node.second))
      }
      weights[node] = weight
      return weight
    }
    val tree = root ?: return emptyList()
    weigh(tree)
    val result = mutableListOf<TreemapLayout.Tile>()
    fun place(node: Node, left: Double, top: Double, right: Double, bottom: Double) {
      val weight = weights.getValue(node)
      if (weight <= 0) return
      if (node.id != null) {
        result += TreemapLayout.Tile(node.id, left, top, right, bottom)
        return
      }
      val first = checkNotNull(node.first)
      val second = checkNotNull(node.second)
      val fraction = weights.getValue(first) / weight
      if (node.vertical) {
        val edge = left + (right - left) * fraction
        place(first, left, top, edge, bottom)
        place(second, edge, top, right, bottom)
      } else {
        val edge = top + (bottom - top) * fraction
        place(first, left, top, right, edge)
        place(second, left, edge, right, bottom)
      }
    }
    place(tree, 0.0, 0.0, width, height)
    return result
  }
}
