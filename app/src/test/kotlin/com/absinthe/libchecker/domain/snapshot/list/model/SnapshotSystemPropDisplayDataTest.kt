package com.absinthe.libchecker.domain.snapshot.list.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotSystemPropDisplayDataTest {

  @Test
  fun buildsDescriptionFromLabelAndDisplayValue() {
    val data = buildSnapshotSystemPropDisplayData(
      label = "Build ID",
      displayValue = "old -> new"
    )

    assertEquals("Build ID", data.label)
    assertEquals("old -> new", data.displayValue)
    assertEquals("Build ID, old -> new", data.description)
  }

  @Test
  fun skipsBlankDescriptionParts() {
    val data = buildSnapshotSystemPropDisplayData(
      label = " ",
      displayValue = "2026-01-01 -> 2026-02-01"
    )

    assertEquals("2026-01-01 -> 2026-02-01", data.description)
  }
}
