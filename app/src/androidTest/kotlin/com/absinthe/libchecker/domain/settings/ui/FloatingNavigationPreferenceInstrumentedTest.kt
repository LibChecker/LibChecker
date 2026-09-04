package com.absinthe.libchecker.domain.settings.ui

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.view.app.BlurCoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationBarView
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingNavigationPreferenceInstrumentedTest {
  @Test
  fun appbarLiftStateSurvivesAppearanceLayoutChanges() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    var activity: MainActivity? = null
    try {
      instrumentation.targetContext.startActivity(
        Intent(instrumentation.targetContext, MainActivity::class.java)
          .setAction(Intent.ACTION_APPLICATION_PREFERENCES)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      )
      val launchedActivity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as MainActivity
      activity = launchedActivity
      assertTrue(
        waitUntil(instrumentation) {
          launchedActivity.settingsFragment()?.let { settings ->
            settings.isResumed && launchedActivity.settingsRecyclerView()?.isLaidOut == true
          } == true
        }
      )
      instrumentation.runOnMainSync {
        if (launchedActivity.preferenceChecked(Constants.PREF_BLUR_DESIGN)) {
          launchedActivity.togglePreference(Constants.PREF_BLUR_DESIGN)
        }
      }
      assertTrue(
        waitUntil(instrumentation) {
          (launchedActivity.findViewById<View>(R.id.container) as? BlurCoordinatorLayout)?.blurEnabled == false
        }
      )
      instrumentation.runOnMainSync {
        val settingsList = requireNotNull(launchedActivity.settingsRecyclerView())
        settingsList.scrollBy(0, settingsList.height)
      }
      assertTrue(
        "AppBar did not lift after scrolling settings",
        waitUntil(instrumentation) {
          launchedActivity.findViewById<AppBarLayout>(R.id.appbar).isLifted
        }
      )

      instrumentation.runOnMainSync {
        launchedActivity.togglePreference(Constants.PREF_FLOATING_NAV_BAR)
      }
      assertAppbarRemainsLiftedDuringTransition(
        instrumentation,
        launchedActivity,
        "Toggling floating navigation cleared the lifted AppBar"
      )
      instrumentation.runOnMainSync {
        launchedActivity.togglePreference(Constants.PREF_FLOATING_NAV_BAR)
      }
      assertAppbarRemainsLiftedDuringTransition(
        instrumentation,
        launchedActivity,
        "Toggling floating navigation back cleared the lifted AppBar"
      )

      instrumentation.runOnMainSync {
        launchedActivity.togglePreference(Constants.PREF_BLUR_DESIGN)
      }
      assertAppbarVisualStateRemainsRaised(
        instrumentation,
        launchedActivity,
        "Enabling blur cleared the raised AppBar"
      )
      assertTrue(
        waitUntil(instrumentation) {
          (launchedActivity.findViewById<View>(R.id.container) as? BlurCoordinatorLayout)?.blurEnabled == true
        }
      )
      val blurContainer = launchedActivity.findViewById<BlurCoordinatorLayout>(R.id.container)
      instrumentation.runOnMainSync {
        launchedActivity.togglePreference(Constants.PREF_FLOATING_NAV_BAR)
      }
      assertAppbarVisualStateRemainsRaised(
        instrumentation,
        launchedActivity,
        "Toggling floating navigation under blur cleared the raised AppBar"
      )
      instrumentation.runOnMainSync {
        launchedActivity.togglePreference(Constants.PREF_FLOATING_NAV_BAR)
      }
      assertAppbarVisualStateRemainsRaised(
        instrumentation,
        launchedActivity,
        "Toggling floating navigation back under blur cleared the raised AppBar"
      )
      instrumentation.runOnMainSync {
        launchedActivity.togglePreference(Constants.PREF_BLUR_DESIGN)
      }
      assertAppbarVisualStateRemainsRaised(
        instrumentation,
        launchedActivity,
        "Disabling blur cleared the lifted AppBar"
      )
      assertSame(
        "Disabling blur reattached the AppBar through a new parent",
        blurContainer,
        launchedActivity.findViewById<View>(R.id.container)
      )
    } finally {
      instrumentation.runOnMainSync { activity?.finish() }
      instrumentation.removeMonitor(monitor)
    }
  }

  @Test
  fun preferenceRemainsVisibleWhenSettingsFragmentIsRestoredAfterRotation() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val initialMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    var recreationMonitor: Instrumentation.ActivityMonitor? = null
    var activity: MainActivity? = null
    try {
      assertTrue(instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0))
      instrumentation.targetContext.startActivity(
        Intent(instrumentation.targetContext, MainActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      )
      val initialActivity = instrumentation.waitForMonitorWithTimeout(initialMonitor, 10_000L) as MainActivity
      activity = initialActivity
      instrumentation.runOnMainSync {
        initialActivity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_settings
      }
      assertTrue(waitUntil(instrumentation) { initialActivity.floatingNavigationPreferenceVisible() })

      recreationMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
      assertTrue(instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90))
      val recreatedActivity = instrumentation.waitForMonitorWithTimeout(recreationMonitor, 10_000L) as? MainActivity
      assertNotNull("MainActivity was not recreated after rotation", recreatedActivity)
      activity = recreatedActivity
      assertTrue(
        "Floating navigation preference disappeared after SettingsFragment restoration",
        waitUntil(instrumentation) { recreatedActivity?.floatingNavigationPreferenceVisible() == true }
      )
    } finally {
      instrumentation.runOnMainSync { activity?.finish() }
      recreationMonitor?.let(instrumentation::removeMonitor)
      instrumentation.removeMonitor(initialMonitor)
      instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }
  }

  @Test
  fun landscapeBlurCanvasExtendsBehindFloatingRail() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val initialMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    var recreationMonitor: Instrumentation.ActivityMonitor? = null
    var activity: MainActivity? = null
    try {
      assertTrue(instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0))
      instrumentation.targetContext.startActivity(
        Intent(instrumentation.targetContext, MainActivity::class.java)
          .setAction(Intent.ACTION_APPLICATION_PREFERENCES)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      )
      val initialActivity = instrumentation.waitForMonitorWithTimeout(initialMonitor, 10_000L) as MainActivity
      activity = initialActivity
      assertTrue(waitUntil(instrumentation) { initialActivity.findViewById<View>(R.id.container).isLaidOut })

      recreationMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
      assertTrue(instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90))
      val launchedActivity = instrumentation.waitForMonitorWithTimeout(recreationMonitor, 10_000L) as MainActivity
      activity = launchedActivity
      assertTrue(
        waitUntil(instrumentation) {
          launchedActivity.findViewById<View>(R.id.container).isLaidOut &&
            launchedActivity.findViewById<View>(R.id.nav_view).isLaidOut
        }
      )
      instrumentation.runOnMainSync {
        launchedActivity.setBlurDesignEnabled(true)
        launchedActivity.setFloatingNavBarEnabled(true)
      }
      assertTrue(
        "Landscape blur canvas did not cover the area behind the floating rail",
        waitUntil(instrumentation) {
          val container = launchedActivity.findViewById<View>(R.id.container)
          val rail = launchedActivity.findViewById<View>(R.id.nav_view)
          container.left == 0 && container.paddingStart == rail.right
        }
      )
    } finally {
      instrumentation.runOnMainSync { activity?.finish() }
      recreationMonitor?.let(instrumentation::removeMonitor)
      instrumentation.removeMonitor(initialMonitor)
      instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }
  }

  private fun MainActivity.floatingNavigationPreferenceVisible(): Boolean {
    val settings = settingsFragment() ?: return false
    return settings.findPreference<androidx.preference.Preference>(Constants.PREF_FLOATING_NAV_BAR)?.isVisible == true
  }

  private fun MainActivity.settingsRecyclerView(): RecyclerView? {
    return settingsFragment()?.view?.findViewById(android.R.id.list)
  }

  private fun MainActivity.settingsFragment(): SettingsFragment? {
    return supportFragmentManager.fragments.firstNotNullOfOrNull { it.findSettingsFragment() }
  }

  private fun MainActivity.preferenceChecked(key: String): Boolean {
    return requireNotNull(settingsFragment()?.findPreference<TwoStatePreference>(key)).isChecked
  }

  private fun MainActivity.togglePreference(key: String) {
    val preference = requireNotNull(settingsFragment()?.findPreference<TwoStatePreference>(key))
    val checked = !preference.isChecked
    check(preference.callChangeListener(checked))
    preference.isChecked = checked
  }

  private fun assertAppbarRemainsLiftedDuringTransition(
    instrumentation: Instrumentation,
    activity: MainActivity,
    message: String
  ) {
    val isLifted = AtomicBoolean(false)
    val startedAt = SystemClock.uptimeMillis()
    var state = ""
    val deadline = SystemClock.uptimeMillis() + 1_000L
    while (SystemClock.uptimeMillis() < deadline) {
      instrumentation.runOnMainSync {
        val appbar = activity.findViewById<AppBarLayout>(R.id.appbar)
        val recyclerView = activity.settingsRecyclerView()
        isLifted.set(appbar.isLifted)
        state =
          "after ${SystemClock.uptimeMillis() - startedAt} ms; " +
          "liftOnScroll=${appbar.isLiftOnScroll}; " +
          "container=${activity.findViewById<View>(R.id.container).javaClass.simpleName}; " +
          "canScrollUp=${recyclerView?.canScrollVertically(-1)}; " +
          "children=${recyclerView?.childCount}; layoutRequested=${recyclerView?.isLayoutRequested}"
      }
      assertTrue("$message ($state)", isLifted.get())
      SystemClock.sleep(16L)
    }
  }

  private fun assertAppbarVisualStateRemainsRaised(
    instrumentation: Instrumentation,
    activity: MainActivity,
    message: String
  ) {
    val remainsRaised = AtomicBoolean(false)
    val startedAt = SystemClock.uptimeMillis()
    var state = ""
    val deadline = SystemClock.uptimeMillis() + 1_000L
    while (SystemClock.uptimeMillis() < deadline) {
      instrumentation.runOnMainSync {
        val appbar = activity.findViewById<AppBarLayout>(R.id.appbar)
        val container = activity.findViewById<View>(R.id.container)
        val underlaps = (container as? BlurCoordinatorLayout)?.let {
          BlurCoordinatorLayout::class.java.getDeclaredField("contentUnderlapsAppbar").run {
            isAccessible = true
            getBoolean(it)
          }
        }
        remainsRaised.set(appbar.isLifted || underlaps == true)
        state =
          "after ${SystemClock.uptimeMillis() - startedAt} ms; " +
          "lifted=${appbar.isLifted}; underlaps=$underlaps; container=${container.javaClass.simpleName}"
      }
      assertTrue("$message ($state)", remainsRaised.get())
      SystemClock.sleep(16L)
    }
  }

  private fun Fragment.findSettingsFragment(): SettingsFragment? {
    if (this is SettingsFragment) return this
    return childFragmentManager.fragments.firstNotNullOfOrNull { it.findSettingsFragment() }
  }

  private fun waitUntil(
    instrumentation: Instrumentation,
    condition: () -> Boolean
  ): Boolean {
    val conditionMet = AtomicBoolean(false)
    val deadline = SystemClock.uptimeMillis() + 10_000L
    while (!conditionMet.get() && SystemClock.uptimeMillis() < deadline) {
      instrumentation.runOnMainSync { conditionMet.set(condition()) }
      if (!conditionMet.get()) SystemClock.sleep(16L)
    }
    return conditionMet.get()
  }
}
