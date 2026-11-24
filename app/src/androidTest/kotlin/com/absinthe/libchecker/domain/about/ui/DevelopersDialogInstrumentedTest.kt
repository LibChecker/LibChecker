package com.absinthe.libchecker.domain.about.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevelopersDialogInstrumentedTest {
  @Test
  fun dialogCanBeDismissedImmediatelyAfterOpening() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val dialog = DevelopersDialogFragment()
        dialog.showNow(activity.supportFragmentManager, "developers")
        dialog.dismissNow()
        assertNull(dialog.view)
      }
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
  }
}
