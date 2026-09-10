package com.absinthe.libchecker.domain.statistics.reference.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreemapLayoutTest {
  @Test
  fun aggregateStaysLastEvenWhenItHasTheLargestArea() {
    val tiles = TreemapLayout.layout(
      listOf(TreemapLayout.Entry("other", 1000.0, sortLast = true), TreemapLayout.Entry("a", 10.0), TreemapLayout.Entry("b", 20.0)),
      400.0,
      800.0
    )
    assertEquals(listOf("b", "a", "other"), tiles.map { it.id })
    tiles.forEach { tile ->
      val weight = when (tile.id) {
        "a" -> 10.0
        "b" -> 20.0
        else -> 1000.0
      }
      assertEquals(400.0 * 800.0 * weight / 1030.0, (tile.right - tile.left) * (tile.bottom - tile.top), 0.0001)
    }
  }

  @Test
  fun layoutPreservesAreaAndBoundsWithoutOverlap() {
    listOf(
      listOf(1.0),
      listOf(6.0, 3.0, 1.0),
      List(300) { if (it == 0) 10000.0 else 1.0 }
    ).forEach { weights ->
      val entries = weights.mapIndexed { index, weight -> TreemapLayout.Entry(index.toString(), weight) }
      listOf(360.0 to 720.0, 720.0 to 360.0).forEach { (width, height) ->
        val tiles = TreemapLayout.layout(entries, width, height)
        assertEquals(entries.size, tiles.size)
        tiles.forEachIndexed { index, tile ->
          assertTrue(tile.left >= 0 && tile.top >= 0 && tile.right <= width + 1e-8 && tile.bottom <= height + 1e-8)
          assertTrue(tile.right > tile.left && tile.bottom > tile.top)
          val expected = weights[tile.id.toInt()] / weights.sum() * width * height
          assertEquals(expected, (tile.right - tile.left) * (tile.bottom - tile.top), 1e-6)
          tiles.drop(index + 1).forEach { other ->
            val overlapWidth = minOf(tile.right, other.right) - maxOf(tile.left, other.left)
            val overlapHeight = minOf(tile.bottom, other.bottom) - maxOf(tile.top, other.top)
            assertTrue(overlapWidth <= 1e-8 || overlapHeight <= 1e-8)
          }
        }
        assertEquals(tiles, TreemapLayout.layout(entries.reversed(), width, height))
      }
    }
  }

  @Test
  fun invalidWeightsAndSizesProduceNoTiles() {
    val invalid = listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)
      .mapIndexed { index, weight -> TreemapLayout.Entry(index.toString(), weight) }
    assertTrue(TreemapLayout.layout(invalid, 100.0, 100.0).isEmpty())
    assertTrue(TreemapLayout.layout(emptyList(), 100.0, 100.0).isEmpty())
    val valid = listOf(TreemapLayout.Entry("one", 1.0))
    assertTrue(TreemapLayout.layout(valid, 0.0, 100.0).isEmpty())
    assertTrue(TreemapLayout.layout(valid, 100.0, Double.NaN).isEmpty())
    assertEquals(1, TreemapLayout.layout(invalid + valid, 100.0, 100.0).size)
  }
}
