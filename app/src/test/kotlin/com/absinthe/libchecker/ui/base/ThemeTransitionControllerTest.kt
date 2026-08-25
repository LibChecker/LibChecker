package com.absinthe.libchecker.ui.base

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTransitionControllerTest {

  @Test
  fun `forced dark requires a configuration change from light`() {
    assertTrue(
      nightModeRequiresConfigurationChange(
        currentUiMode = Configuration.UI_MODE_NIGHT_NO,
        applicationUiMode = Configuration.UI_MODE_NIGHT_NO,
        nightMode = AppCompatDelegate.MODE_NIGHT_YES
      )
    )
  }

  @Test
  fun `following an already matching system mode does not require recreation`() {
    assertFalse(
      nightModeRequiresConfigurationChange(
        currentUiMode = Configuration.UI_MODE_NIGHT_YES,
        applicationUiMode = Configuration.UI_MODE_NIGHT_YES,
        nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      )
    )
  }

  @Test
  fun `following a different system mode requires recreation`() {
    assertTrue(
      nightModeRequiresConfigurationChange(
        currentUiMode = Configuration.UI_MODE_NIGHT_NO,
        applicationUiMode = Configuration.UI_MODE_NIGHT_YES,
        nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      )
    )
  }

  @Test
  fun `recreate setting change waits for the frozen frame`() {
    val events = mutableListOf<String>()
    val transition = FrozenFrameTransitionState()

    assertTrue(transition.begin())
    assertTrue(events.isEmpty())

    transition.onFrameReady {
      events += "setting changed"
    }

    assertEquals(listOf("setting changed"), events)
  }

  @Test
  fun `recreate transition rejects another request while active`() {
    val transition = FrozenFrameTransitionState()

    assertTrue(transition.begin())
    assertFalse(transition.begin())
  }
}
