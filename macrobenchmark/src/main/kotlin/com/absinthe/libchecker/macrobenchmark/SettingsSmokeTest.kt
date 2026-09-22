package com.absinthe.libchecker.macrobenchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSmokeTest {
  @Test
  fun minifiedSettingsInflateAndResume() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val launch = "am start -W -a android.intent.action.APPLICATION_PREFERENCES " +
      "-n com.absinthe.libchecker.debug/com.absinthe.libchecker.domain.home.ui.MainActivity"
    device.executeShellCommand("am force-stop com.absinthe.libchecker.debug")
    repeat(2) {
      device.executeShellCommand(launch)
      assertTrue(
        "Minified settings did not display preference rows",
        device.wait(
          Until.hasObject(
            By.res("android", "list")
              .pkg("com.absinthe.libchecker.debug")
              .hasDescendant(By.clickable(true).desc(Pattern.compile(".+", Pattern.DOTALL)))
          ),
          10_000L
        )
      )
      device.pressHome()
      device.waitForIdle()
    }
  }
}
