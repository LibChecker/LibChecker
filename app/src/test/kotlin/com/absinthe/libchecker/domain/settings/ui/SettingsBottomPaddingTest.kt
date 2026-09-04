package com.absinthe.libchecker.domain.settings.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBottomPaddingTest {

  @Test
  fun `padding follows navigation presence and margin`() {
    assertEquals(104, calculateSettingsBottomPadding(8, 24, 96))
    assertEquals(32, calculateSettingsBottomPadding(8, 24, null))
    assertEquals(104, calculateSettingsBottomPadding(8, 24, 72, 24))
  }
}
