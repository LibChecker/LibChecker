package com.absinthe.libchecker.domain.app.detail.feature

import org.junit.Assert.assertEquals
import org.junit.Test

class R8VersionFormatterTest {
  @Test
  fun prereleasesSortDescendingByStageAndNumericSuffixAfterStableRelease() {
    assertEquals(
      "9.6.0 / 9.6.0-rc01 / 9.6.0-beta02 / 9.6.0-alpha10 / 9.6.0-alpha3 / 9.6.0-alpha02 / 9.6.0-dev",
      formatR8Versions(listOf("9.6.0-alpha02", "9.6.0-dev", "9.6.0-alpha10", "9.6.0", "9.6.0-beta02", "9.6.0-alpha3", "9.6.0-rc01"))
    )
  }

  @Test
  fun sortsNumericVersionsDescendingAndPreservesModes() {
    assertEquals(
      "10.0.1 / 9.10.2 (Full) / 9.10.2 (Compatibility) / 9.10.2-dev / 9.9.30 / 8.3.37",
      formatR8Versions(listOf("8.3.37", "9.9.30", "9.10.2-dev", "9.10.2 (Full)", "10.0.1", "9.10.2 (Compatibility)"))
    )
  }
}
