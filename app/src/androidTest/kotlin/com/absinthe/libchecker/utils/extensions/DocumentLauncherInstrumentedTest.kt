package com.absinthe.libchecker.utils.extensions

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentLauncherInstrumentedTest {
  @Test
  fun missingPickerIsHandledAndSuccessfulLaunchKeepsInput() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val launcher = TestLauncher()
      launcher.failure = ActivityNotFoundException("No document provider")
      assertThrows(ActivityNotFoundException::class.java) { launcher.launch("backup.zip") }
      assertFalse(launcher.launchDocument(instrumentation.targetContext, "backup.zip"))
      launcher.failure = null
      assertTrue(launcher.launchDocument(instrumentation.targetContext, "backup.zip"))
      assertEquals("backup.zip", launcher.input)
    }
  }

  @Test
  fun unrelatedLaunchFailuresAreNotHidden() {
    val launcher = TestLauncher().apply { failure = IllegalStateException("Unregistered launcher") }
    assertThrows(IllegalStateException::class.java) {
      launcher.launchDocument(InstrumentationRegistry.getInstrumentation().targetContext, "backup.zip")
    }
  }

  private class TestLauncher : ActivityResultLauncher<String>() {
    var failure: RuntimeException? = null
    var input: String? = null

    override fun launch(input: String, options: ActivityOptionsCompat?) {
      failure?.let { throw it }
      this.input = input
    }

    override fun unregister() = Unit

    override val contract: ActivityResultContract<String, *> = ActivityResultContracts.CreateDocument("*/*")
  }
}
