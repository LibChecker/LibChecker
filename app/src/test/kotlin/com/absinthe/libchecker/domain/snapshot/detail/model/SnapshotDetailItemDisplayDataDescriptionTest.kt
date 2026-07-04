package com.absinthe.libchecker.domain.snapshot.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailItemDisplayDataDescriptionTest {

  @Test
  fun joinsStatusTitleExtraAndRuleLabel() {
    assertEquals(
      "Changed, libfoo.so, 42 KB, JNI",
      buildSnapshotDetailItemDescription(
        statusLabel = "Changed",
        title = "libfoo.so",
        extra = "42 KB",
        ruleLabel = "JNI"
      )
    )
  }

  @Test
  fun skipsBlankParts() {
    assertEquals(
      "Added, com.example.MainActivity",
      buildSnapshotDetailItemDescription(
        statusLabel = "Added",
        title = "com.example.MainActivity",
        extra = " ",
        ruleLabel = null
      )
    )
  }
}
