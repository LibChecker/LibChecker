package com.absinthe.libchecker.domain.snapshot.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailDiffTextStyleTest {

  @Test
  fun findsEveryDiffArrowForEmphasis() {
    assertEquals(
      listOf(4, 10),
      snapshotDetailDiffArrowIndices("old → new → final")
    )
  }

  @Test
  fun ignoresTextWithoutDiffArrows() {
    assertEquals(emptyList<Int>(), snapshotDetailDiffArrowIndices("com.example.app"))
  }
}
