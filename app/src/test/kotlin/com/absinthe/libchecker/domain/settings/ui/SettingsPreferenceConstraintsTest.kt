package com.absinthe.libchecker.domain.settings.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPreferenceConstraintsTest {

  @Test
  fun `threshold restored below range is clamped to minimum`() {
    assertEquals(LIB_REFERENCE_THRESHOLD_MIN, normalizeLibReferenceThreshold(0))
  }

  @Test
  fun `threshold restored above range is clamped to maximum`() {
    assertEquals(LIB_REFERENCE_THRESHOLD_MAX, normalizeLibReferenceThreshold(51))
  }

  @Test
  fun `valid threshold is preserved`() {
    assertEquals(20, normalizeLibReferenceThreshold(20))
  }
}
