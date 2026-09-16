package com.absinthe.libchecker.domain.settings.ui

import android.content.Intent
import android.os.SystemClock
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceSettingsNavigationInstrumentedTest {
  @Test
  fun appearanceSurvivesRecreationAndReturnsToTheSameSettingsPosition() {
    val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
      .setAction(Intent.ACTION_APPLICATION_PREFERENCES)
    ActivityScenario.launch<MainActivity>(intent).use { scenario ->
      scenario.await { it.currentSettings() is SettingsFragment && it.currentSettings()?.listView?.childCount != 0 }
      var firstPosition = 0
      var firstTop = 0
      scenario.onActivity { activity ->
        val root = activity.currentSettings() as SettingsFragment
        assertNull(root.findPreference<Preference>(Constants.PREF_DARK_MODE))
        val general = requireNotNull(root.findPreference<PreferenceCategory>("normalCategory"))
        assertEquals(Constants.PREF_APK_ANALYTICS, general.getPreference(0).key)
        assertEquals(APPEARANCE_PREFERENCE_KEY, general.getPreference(1).key)
        root.listView.scrollBy(0, 70)
        val layout = root.listView.layoutManager as LinearLayoutManager
        firstPosition = layout.findFirstVisibleItemPosition()
        firstTop = requireNotNull(layout.findViewByPosition(firstPosition)).top
        val entry = requireNotNull(root.findPreference<Preference>(APPEARANCE_PREFERENCE_KEY))
        assertTrue(entry.onPreferenceClickListener!!.onPreferenceClick(entry))
      }
      scenario.await { it.currentSettings() is AppearanceSettingsFragment && it.currentSettings()?.listView?.childCount != 0 }
      scenario.onActivity { activity ->
        val appearance = activity.currentSettings() as AppearanceSettingsFragment
        listOf(
          Constants.PREF_DARK_MODE,
          Constants.PREF_AMOLED_THEME,
          Constants.PREF_LOCALE,
          Constants.PREF_COLORFUL_ICON,
          Constants.PREF_BLUR_DESIGN,
          Constants.PREF_FLOATING_NAV_BAR
        ).forEach { assertNotNull(appearance.findPreference<Preference>(it)) }
        assertNull(appearance.findPreference<Preference>(Constants.PREF_SNAPSHOT_KEEP))
        assertNotNull(activity.findViewById<Toolbar>(R.id.toolbar).navigationIcon)
      }

      scenario.recreate()
      scenario.await { it.currentSettings() is AppearanceSettingsFragment && it.currentSettings()?.listView?.childCount != 0 }
      scenario.onActivity { activity ->
        assertNotNull(activity.findViewById<Toolbar>(R.id.toolbar).navigationIcon)
        assertEquals(R.id.navigation_settings, activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId)
        activity.onBackPressedDispatcher.onBackPressed()
      }
      scenario.await { it.currentSettings() is SettingsFragment && it.currentSettings()?.listView?.childCount != 0 }
      scenario.onActivity { activity ->
        val root = activity.currentSettings() as SettingsFragment
        val layout = root.listView.layoutManager as LinearLayoutManager
        assertEquals(firstPosition, layout.findFirstVisibleItemPosition())
        assertEquals(firstTop, requireNotNull(layout.findViewByPosition(firstPosition)).top)
        assertNull(activity.findViewById<Toolbar>(R.id.toolbar).navigationIcon)
        activity.settingsContainer().openAppearance()
      }
      scenario.await { it.currentSettings() is AppearanceSettingsFragment }
      scenario.onActivity { it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_app_list }
      scenario.await {
        it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId == R.id.navigation_app_list &&
          it.findViewById<Toolbar>(R.id.toolbar).navigationIcon == null
      }
      scenario.onActivity { it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_settings }
      scenario.await { it.currentSettings() is SettingsFragment && it.currentSettings()?.isResumed == true }
      scenario.onActivity { assertFalse(it.settingsContainer().isAppearanceVisible) }
    }
  }

  private fun MainActivity.settingsContainer(): SettingsContainerFragment = supportFragmentManager.fragments.filterIsInstance<SettingsContainerFragment>().single()

  private fun MainActivity.currentSettings(): BaseSettingsFragment? = (
    supportFragmentManager.fragments.filterIsInstance<SettingsContainerFragment>().firstOrNull()
      ?.childFragmentManager?.findFragmentById(R.id.settings_content) as? BaseSettingsFragment
    )
    ?.takeIf { it.isResumed }

  private fun ActivityScenario<MainActivity>.await(condition: (MainActivity) -> Boolean) {
    val deadline = SystemClock.uptimeMillis() + 10_000L
    do {
      var ready = false
      onActivity { ready = condition(it) }
      if (ready) return
      SystemClock.sleep(16)
    } while (SystemClock.uptimeMillis() < deadline)
    throw AssertionError("Settings screen did not reach the expected state")
  }
}
