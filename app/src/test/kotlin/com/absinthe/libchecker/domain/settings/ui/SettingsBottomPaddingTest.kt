package com.absinthe.libchecker.domain.settings.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBottomPaddingTest {

  @Test
  fun `bottom navigation height replaces system bar inset`() {
    assertEquals(
      104,
      calculateSettingsBottomPadding(
        basePadding = 8,
        systemBarBottomInset = 24,
        bottomNavigationHeight = 96
      )
    )
  }

  @Test
  fun `system bar inset is used without bottom navigation`() {
    assertEquals(
      32,
      calculateSettingsBottomPadding(
        basePadding = 8,
        systemBarBottomInset = 24,
        bottomNavigationHeight = null
      )
    )
  }
}
