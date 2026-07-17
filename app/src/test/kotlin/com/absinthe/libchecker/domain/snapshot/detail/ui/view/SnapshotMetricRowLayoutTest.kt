package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotMetricRowLayoutTest {

  @Test
  fun alignsLabelAndMultilineValueToTheSameFirstBaseline() {
    val row = planSnapshotMetricRowLayout(
      labelHeight = 18,
      labelBaseline = 12,
      valueHeight = 40,
      valueBaseline = 16
    )

    assertEquals(4, row.labelTopOffset)
    assertEquals(0, row.valueTopOffset)
    assertEquals(40, row.height)
    assertEquals(16, row.labelTopOffset + 12)
    assertEquals(16, row.valueTopOffset + 16)
  }

  @Test
  fun includesTheShiftedValueBottomInTheMeasuredHeight() {
    val row = planSnapshotMetricRowLayout(
      labelHeight = 24,
      labelBaseline = 18,
      valueHeight = 20,
      valueBaseline = 14
    )

    assertEquals(0, row.labelTopOffset)
    assertEquals(4, row.valueTopOffset)
    assertEquals(24, row.height)
  }
}
