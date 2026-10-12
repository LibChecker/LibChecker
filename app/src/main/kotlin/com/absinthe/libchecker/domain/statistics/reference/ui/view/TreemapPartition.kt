package com.absinthe.libchecker.domain.statistics.reference.ui.view

import kotlin.math.abs

/** Keeps split directions and membership fixed while weights change during a gesture. */
internal class TreemapPartition(entries: List<TreemapLayout.Entry>, width: Double, height: Double) {
  private class Node(val index: Int, val id: String?, val weight: Double, val vertical: Boolean, val first: Node? = null, val second: Node? = null)
  private var nodeCount = 0

  private fun build(tiles: List<TreemapLayout.Tile>, weights: Map<String, Double>): Node {
    if (tiles.size == 1) return Node(nodeCount++, tiles[0].id, weights.getValue(tiles[0].id), false)
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
        return Node(nodeCount++, null, first.weight + second.weight, vertical, first, second)
      }
      prefixRight = maxOf(prefixRight, tiles[split].right)
      prefixBottom = maxOf(prefixBottom, tiles[split].bottom)
    }
    error("Squarified layout must have a full row cut")
  }

  private val root = TreemapLayout.layout(entries, width, height)
    .takeIf { it.isNotEmpty() }
    ?.let { build(it, entries.associateBy(keySelector = { it.id }, valueTransform = { it.weight })) }

  fun layout(width: Double, height: Double, from: Set<String>?, to: Set<String>?, progress: Float): List<TreemapLayout.Tile> {
    if (width <= 0 || height <= 0) return emptyList()
    val result = mutableListOf<TreemapLayout.Tile>()
    prepareTransition(from, to).layout(width, height, progress) { id, left, top, right, bottom ->
      result += TreemapLayout.Tile(id, left, top, right, bottom)
    }
    return result
  }

  fun interface TileConsumer {
    fun accept(id: String, left: Double, top: Double, right: Double, bottom: Double)
  }

  fun prepareTransition(from: Set<String>?, to: Set<String>?): Transition = Transition(from, to)

  inner class Transition internal constructor(from: Set<String>?, to: Set<String>?) {
    // Subtree sums interpolate linearly too, so frames need no weight map or leaf rescan.
    private val fromWeights = DoubleArray(nodeCount)
    private val toWeights = DoubleArray(nodeCount)

    init {
      fun weigh(node: Node) {
        if (node.id != null) {
          fromWeights[node.index] = if (from == null || node.id in from) node.weight else 0.0
          toWeights[node.index] = if (to == null || node.id in to) node.weight else 0.0
        } else {
          val first = checkNotNull(node.first)
          val second = checkNotNull(node.second)
          weigh(first)
          weigh(second)
          fromWeights[node.index] = fromWeights[first.index] + fromWeights[second.index]
          toWeights[node.index] = toWeights[first.index] + toWeights[second.index]
        }
      }
      root?.let(::weigh)
    }

    fun layout(width: Double, height: Double, progress: Float, consumer: TileConsumer) {
      if (width <= 0 || height <= 0) return
      val tree = root ?: return
      place(tree, 0.0, 0.0, width, height, progress.coerceIn(0f, 1f).toDouble(), consumer)
    }

    private fun weight(node: Node, progress: Double): Double = fromWeights[node.index] * (1 - progress) + toWeights[node.index] * progress

    private fun place(node: Node, left: Double, top: Double, right: Double, bottom: Double, progress: Double, consumer: TileConsumer) {
      val weight = weight(node, progress)
      if (weight <= 0) return
      if (node.id != null) {
        consumer.accept(node.id, left, top, right, bottom)
        return
      }
      val first = checkNotNull(node.first)
      val second = checkNotNull(node.second)
      val fraction = weight(first, progress) / weight
      if (node.vertical) {
        val edge = left + (right - left) * fraction
        place(first, left, top, edge, bottom, progress, consumer)
        place(second, edge, top, right, bottom, progress, consumer)
      } else {
        val edge = top + (bottom - top) * fraction
        place(first, left, top, right, edge, progress, consumer)
        place(second, left, edge, right, bottom, progress, consumer)
      }
    }
  }
}
