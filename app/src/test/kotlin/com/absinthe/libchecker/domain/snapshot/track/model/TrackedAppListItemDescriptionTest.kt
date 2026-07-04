package com.absinthe.libchecker.domain.snapshot.track.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackedAppListItemDescriptionTest {

  @Test
  fun joinsLabelAndPackageName() {
    assertEquals(
      "LibChecker, com.absinthe.libchecker",
      buildTrackedAppListItemDescription(
        label = "LibChecker",
        packageName = "com.absinthe.libchecker"
      )
    )
  }

  @Test
  fun skipsBlankParts() {
    assertEquals(
      "com.absinthe.libchecker",
      buildTrackedAppListItemDescription(
        label = " ",
        packageName = "com.absinthe.libchecker"
      )
    )
  }
}
