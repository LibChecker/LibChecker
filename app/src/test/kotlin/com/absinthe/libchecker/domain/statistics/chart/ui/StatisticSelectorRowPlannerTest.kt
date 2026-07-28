package com.absinthe.libchecker.domain.statistics.chart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StatisticSelectorRowPlannerTest {

  @Test
  fun `expansion crossing available width changes affected rows`() {
    val currentRows = calculateFlexRowIndices(
      availableWidth = 168,
      outerWidths = listOf(56, 56, 56)
    )
    val targetRows = calculateFlexRowIndices(
      availableWidth = 168,
      outerWidths = listOf(56, 120, 56)
    )

    assertEquals(listOf(0, 0, 0), currentRows)
    assertEquals(listOf(0, 1, 2), targetRows)
    assertNotEquals(currentRows, targetRows)
    assertEquals(listOf(1, 2), calculateChangedRowIndices(currentRows, targetRows))
  }

  @Test
  fun `expansion within available width keeps the same row`() {
    val currentRows = calculateFlexRowIndices(
      availableWidth = 200,
      outerWidths = listOf(56, 56)
    )
    val targetRows = calculateFlexRowIndices(
      availableWidth = 200,
      outerWidths = listOf(56, 120)
    )

    assertEquals(currentRows, targetRows)
    assertEquals(emptyList<Int>(), calculateChangedRowIndices(currentRows, targetRows))
  }
}
