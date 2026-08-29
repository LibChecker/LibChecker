package com.absinthe.libchecker.domain.settings.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferenceClickTest {

  @Test
  fun `invalid click is rejected without running its action`() {
    var actionInvoked = false

    val handled = dispatchPreferenceClick(isInvalidClick = true) {
      actionInvoked = true
    }

    assertFalse(handled)
    assertFalse(actionInvoked)
  }

  @Test
  fun `valid click runs its action and is handled`() {
    var actionInvoked = false

    val handled = dispatchPreferenceClick(isInvalidClick = false) {
      actionInvoked = true
    }

    assertTrue(handled)
    assertTrue(actionInvoked)
  }
}
