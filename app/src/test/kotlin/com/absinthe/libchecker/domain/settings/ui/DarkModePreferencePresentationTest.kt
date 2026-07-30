package com.absinthe.libchecker.domain.settings.ui

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class DarkModePreferencePresentationTest {

  @Test
  fun `dark mode preference icon follows the selected value`() {
    assertEquals(
      R.drawable.ic_theme_light,
      darkModePreferenceIconRes(Constants.DARK_MODE_OFF)
    )
    assertEquals(
      R.drawable.ic_theme_dark,
      darkModePreferenceIconRes(Constants.DARK_MODE_ON)
    )
    assertEquals(
      R.drawable.ic_theme_system,
      darkModePreferenceIconRes(Constants.DARK_MODE_FOLLOW_SYSTEM)
    )
  }
}
