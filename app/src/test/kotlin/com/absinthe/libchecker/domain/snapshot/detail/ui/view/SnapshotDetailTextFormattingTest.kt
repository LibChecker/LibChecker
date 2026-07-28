package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailTextFormattingTest {

  @Test
  fun breaksLongVersionDiffBeforeArrow() {
    val text = "2.5.5.dev.0960433 (2700) → 2.5.5.dev.51aa6cf (2724)"

    assertEquals(
      listOf(text.indexOf('→') - 1),
      planSnapshotDetailLineBreaks(
        text = text,
        secondaryBreakStart = -1,
        maxWidth = 30f,
        measureText = { it.length.toFloat() }
      )
    )
  }

  @Test
  fun breaksLongSizeDiffBeforeArrowAndDelta() {
    val primary = "17.58 MB (17575814 Bytes) → 16.84 MB (16841281 Bytes)"
    val text = "$primary -735 kB (-734533 Bytes)"
    val secondaryBreakStart = primary.length + 1

    assertEquals(
      listOf(primary.indexOf('→') - 1, primary.length),
      planSnapshotDetailLineBreaks(
        text = text,
        secondaryBreakStart = secondaryBreakStart,
        maxWidth = 32f,
        measureText = { it.length.toFloat() }
      )
    )
  }

  @Test
  fun keepsShortDiffOnOneLineButMovesDeltaBelow() {
    val primary = "10 MB → 9 MB"
    val text = "$primary -1 MB"

    assertEquals(
      listOf(primary.length),
      planSnapshotDetailLineBreaks(
        text = text,
        secondaryBreakStart = primary.length + 1,
        maxWidth = 14f,
        measureText = { it.length.toFloat() }
      )
    )
  }

  @Test
  fun exposesPathBreakOpportunitiesAfterDots() {
    assertEquals(
      listOf(4, 13, 24),
      snapshotTechnicalPathBreakOffsets("com.absinthe.libchecker.MainActivity")
    )
  }
}
