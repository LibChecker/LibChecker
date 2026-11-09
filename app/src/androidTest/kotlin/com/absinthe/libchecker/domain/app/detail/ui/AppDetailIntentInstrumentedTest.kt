package com.absinthe.libchecker.domain.app.detail.ui

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDetailIntentInstrumentedTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val first = "com.android.settings"
  private val second = instrumentation.targetContext.packageName
  private fun detailIntent(name: String) = Intent(instrumentation.targetContext, AppDetailActivity::class.java)
    .putExtra(EXTRA_PACKAGE_NAME, name)

  @Test
  fun legacyNewIntentMustSwitchPackage() {
    ActivityScenario.launch<AppDetailActivity>(detailIntent(first)).use { scenario ->
      scenario.onActivity { activity ->
        assertEquals(first, activity.requirePackageName())
        // Instrumentation forwards to the new overload on API 35+, so invoke the legacy entry directly.
        android.app.Activity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java).apply { isAccessible = true }.invoke(activity, detailIntent(second))
        assertEquals("Legacy intent delivery must switch to the requested app", second, activity.requirePackageName())
        assertEquals(second, activity.intent.getStringExtra(EXTRA_PACKAGE_NAME))
      }
      awaitPackage(scenario, second)
    }
  }

  @Test
  fun reusedActivityMustRetainNewPackageAfterRecreation() {
    ActivityScenario.launch<AppDetailActivity>(detailIntent(first)).use { scenario ->
      var oldActivity: AppDetailActivity? = null
      scenario.onActivity { activity ->
        oldActivity = activity
        activity.startActivity(detailIntent(second))
      }
      var switched = false
      val deadline = SystemClock.uptimeMillis() + 10000
      while (!switched && SystemClock.uptimeMillis() < deadline) {
        scenario.onActivity { activity -> switched = activity.requirePackageName() == second }
        if (!switched) SystemClock.sleep(20)
      }
      assertTrue("Real launch must deliver the second intent before recreation", switched)
      scenario.onActivity { activity ->
        assertTrue("singleTask should reuse the existing Activity", oldActivity === activity)
      }
      awaitPackage(scenario, second)
      scenario.recreate()
      scenario.onActivity { activity ->
        assertEquals("Recreation must retain the latest requested app", second, activity.requirePackageName())
        assertEquals(second, activity.intent.getStringExtra(EXTRA_PACKAGE_NAME))
      }
      awaitPackage(scenario, second)
    }
  }

  private fun awaitPackage(scenario: ActivityScenario<AppDetailActivity>, expected: String) {
    var ready = false
    val deadline = SystemClock.uptimeMillis() + 30_000
    while (!ready && SystemClock.uptimeMillis() < deadline) {
      scenario.onActivity { activity ->
        val model = ViewModelProvider(activity)[DetailViewModel::class.java]
        ready = model.isPackageInfoAvailable() && model.packageName() == expected
      }
      if (!ready) SystemClock.sleep(20)
    }
    assertTrue("The displayed package must finish loading: $expected", ready)
  }
}
