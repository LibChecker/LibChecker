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

  @Test
  fun findsSignedMetricDeltaLines() {
    val text = "10 MB → 12 MB\n+2 MB, +20.0%\n100 classes → 90 classes\n-10 classes, -10.0%"

    assertEquals(
      listOf(
        text.indexOf("+2 MB") until text.indexOf("+2 MB") + "+2 MB, +20.0%".length,
        text.indexOf("-10 classes") until text.length
      ),
      snapshotDetailMetricDeltaRanges(text)
    )
  }
}
