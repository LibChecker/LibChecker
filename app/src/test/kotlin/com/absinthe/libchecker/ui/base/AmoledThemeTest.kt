package com.absinthe.libchecker.ui.base

import android.content.res.Configuration
import com.absinthe.libchecker.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AmoledThemeTest {

  @Test
  fun `enabled AMOLED theme uses pure black overlay at night`() {
    assertEquals(
      R.style.ThemeOverlay_Amoled,
      resolveUserThemeOverlay(true, Configuration.UI_MODE_NIGHT_YES)
    )
  }

  @Test
  fun `enabled AMOLED theme keeps default overlay during the day`() {
    assertEquals(
      R.style.ThemeOverlay,
      resolveUserThemeOverlay(true, Configuration.UI_MODE_NIGHT_NO)
    )
  }

  @Test
  fun `disabled AMOLED theme keeps default overlay at night`() {
    assertEquals(
      R.style.ThemeOverlay,
      resolveUserThemeOverlay(false, Configuration.UI_MODE_NIGHT_YES)
    )
  }
}
