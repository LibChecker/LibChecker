package com.absinthe.libchecker.domain.statistics.reference.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreemapPartitionTest {
  @Test
  fun similarWeightsDoNotBecomeThinStrips() {
    val entries = listOf(102.0, 101.0, 100.0).mapIndexed { index, weight -> TreemapLayout.Entry("$index", weight) }
    for ((width, height) in listOf(200.0 to 300.0, 300.0 to 200.0)) {
      val tiles = TreemapPartition(entries, width, height).layout(width, height, null, null, 1f)
      tiles.forEach { tile ->
        val w = tile.right - tile.left
        val h = tile.bottom - tile.top
        assertTrue("Similar weights should remain compact: $w x $h", maxOf(w / h, h / w) < 2.1)
      }
      assertEquals(width * height, tiles.sumOf { (it.right - it.left) * (it.bottom - it.top) }, 0.001)
    }
  }

  @Test
  fun changingRegionKeepsEveryFrameTiledWithoutOverlap() {
    val entries = (1..30).map { TreemapLayout.Entry("$it", it.toDouble()) }
    val partition = TreemapPartition(entries, 400.0, 800.0)
    val baseline = partition.layout(400.0, 800.0, null, null, 1f)
    val compact = TreemapLayout.layout(entries, 400.0, 800.0)
    baseline.zip(compact).forEach { (actual, expected) ->
      assertEquals(expected.id, actual.id)
      assertEquals(expected.left, actual.left, 0.000001)
      assertEquals(expected.top, actual.top, 0.000001)
      assertEquals(expected.right, actual.right, 0.000001)
      assertEquals(expected.bottom, actual.bottom, 0.000001)
    }
    val selected = setOf("4", "13", "22")
    for (step in 0..100) {
      val tiles = partition.layout(400.0, 800.0, null, selected, step / 100f)
      assertEquals(320000.0, tiles.sumOf { (it.right - it.left) * (it.bottom - it.top) }, 0.001)
      tiles.forEachIndexed { index, tile ->
        assertTrue(tile.left >= 0 && tile.top >= 0 && tile.right <= 400 && tile.bottom <= 800)
        tiles.drop(index + 1).forEach { other ->
          val overlapWidth = minOf(tile.right, other.right) - maxOf(tile.left, other.left)
          val overlapHeight = minOf(tile.bottom, other.bottom) - maxOf(tile.top, other.top)
          assertTrue(overlapWidth <= 0.000001 || overlapHeight <= 0.000001)
        }
      }
    }
    assertEquals(selected, partition.layout(400.0, 800.0, null, selected, 1f).map { it.id }.toSet())
    assertEquals(partition.layout(400.0, 800.0, null, selected, 0.3f), partition.layout(400.0, 800.0, selected, null, 0.7f))
  }
}
