package com.absinthe.libchecker.domain.app.list.ui

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.home.ui.MainActivity
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppListFlipLifecycleInstrumentedTest {
  @Test
  fun recreatingViewReusesStableAdapterWithoutRetainingOldList() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      scenario.onActivity { activity ->
        val fragment = activity.supportFragmentManager.findFragmentByTag("f0") as AppListFragment
        val adapter = checkNotNull(fragment.binding.list.adapter)
        assertTrue(adapter.hasStableIds())
        repeat(2) {
          val oldList = fragment.binding.list
          activity.supportFragmentManager.beginTransaction().detach(fragment).commitNow()
          assertFalse(fragment.isBindingInitialized())
          assertNull("The destroyed list must release its adapter", oldList.adapter)
          activity.supportFragmentManager.beginTransaction().attach(fragment).commitNow()
          assertTrue(fragment.isBindingInitialized())
          assertSame(adapter, fragment.binding.list.adapter)
          assertTrue(adapter.hasStableIds())
        }
      }
    }
  }

  @Test
  fun queuedFlipIsCancelledWhenViewIsDestroyed() {
    val flip = AppListFragment::class.java.getDeclaredMethod("flip", Int::class.javaPrimitiveType).apply {
      isAccessible = true
    }
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      val deadline = SystemClock.uptimeMillis() + 30_000
      var ready = false
      while (!ready && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(100)
        scenario.onActivity {
          ready = (it.supportFragmentManager.findFragmentByTag("f0") as? AppListFragment)?.isBindingInitialized() == true
        }
      }
      assertTrue("App list must have a live view", ready)
      lateinit var fragment: AppListFragment
      scenario.onActivity {
        fragment = it.supportFragmentManager.findFragmentByTag("f0") as AppListFragment
      }
      lateinit var live: Job
      scenario.onActivity {
        fragment.binding.vfContainer.displayedChild = VF_INIT
        live = flip.invoke(fragment, VF_LIST) as Job
      }
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      scenario.onActivity {
        assertTrue("A flip for the live view must finish", live.isCompleted)
        assertFalse(live.isCancelled)
        assertEquals(VF_LIST, fragment.binding.vfContainer.displayedChild)
        val pending = flip.invoke(fragment, VF_INIT) as Job
        try {
          assertFalse("The flip must still be queued", pending.isCompleted)
          it.supportFragmentManager.beginTransaction().detach(fragment).commitNow()
          assertFalse(fragment.isBindingInitialized())
          assertTrue("Destroying the view must cancel its queued flip", pending.isCancelled)
        } finally {
          pending.cancel()
        }
        val late = flip.invoke(fragment, VF_LIST) as? Job
        try {
          assertNull("A late flip must not launch without a view", late)
        } finally {
          late?.cancel()
        }
      }
    }
  }
}
