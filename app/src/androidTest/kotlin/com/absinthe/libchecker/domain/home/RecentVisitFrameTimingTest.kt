package com.absinthe.libchecker.domain.home

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.os.Trace
import android.view.Choreographer
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitsPopup
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run in a fresh instrumentation process to compare the first hold with subsequent holds. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class RecentVisitFrameTimingTest {
  @Test
  fun firstAndRepeatedListHolds() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    instrumentation.targetContext.startActivity(
      Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
    val activity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as MainActivity
    val popupField = MainActivity::class.java.getDeclaredField("recentVisitsPopup").apply { isAccessible = true }
    val frames = mutableListOf<Long>()
    val callback = object : Choreographer.FrameCallback {
      override fun doFrame(frameTimeNanos: Long) {
        frames += frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
      }
    }
    val holdMs = maxOf(750L, ViewConfiguration.getLongPressTimeout().toLong() + 200L)
    fun pointer(action: Int, down: Long, x: Float, y: Float) {
      val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
      event.source = InputDevice.SOURCE_TOUCHSCREEN
      val injected = instrumentation.uiAutomation.injectInputEvent(event, true)
      event.recycle()
      if (action != MotionEvent.ACTION_CANCEL) assertTrue("Pointer injection failed", injected)
    }
    try {
      instrumentation.runOnMainSync { activity.findViewById<View>(R.id.navigation_app_list).performClick() }
      val deadline = SystemClock.uptimeMillis() + 10_000
      var ready = false
      while (!ready && SystemClock.uptimeMillis() < deadline) {
        instrumentation.runOnMainSync {
          ready = activity.findViewById<RecyclerView>(android.R.id.list)?.let { it.isShown && it.childCount > 1 } == true
        }
        SystemClock.sleep(50)
      }
      assertTrue(ready)
      SystemClock.sleep(1000)
      repeat(4) { index ->
        val bounds = Rect()
        instrumentation.runOnMainSync {
          assertTrue(activity.findViewById<RecyclerView>(android.R.id.list).getChildAt(0).getGlobalVisibleRect(bounds))
          frames.clear()
          Choreographer.getInstance().postFrameCallback(callback)
          Trace.beginAsyncSection("ShortcutHold$index", index)
        }
        val start = System.nanoTime()
        val down = SystemClock.uptimeMillis()
        pointer(MotionEvent.ACTION_DOWN, down, bounds.centerX().toFloat(), bounds.centerY().toFloat())
        SystemClock.sleep(holdMs + 550)
        instrumentation.runOnMainSync {
          Trace.endAsyncSection("ShortcutHold$index", index)
          Choreographer.getInstance().removeFrameCallback(callback)
          assertTrue((popupField.get(activity) as? RecentVisitsPopup)?.isShowing == true)
          val intervals = frames.zipWithNext { a, b -> a to (b - a) / 1_000_000.0 }
            .filter { (time, _) -> time >= start + (holdMs - 100) * 1_000_000 }
            .map { it.second }
          println("SHORTCUT_FRAMES round=$index intervals_ms=${intervals.joinToString(",")}")
        }
        pointer(MotionEvent.ACTION_UP, down, bounds.centerX().toFloat(), bounds.centerY().toFloat())
        SystemClock.sleep(700)
      }
    } finally {
      pointer(MotionEvent.ACTION_CANCEL, SystemClock.uptimeMillis(), 0f, 0f)
      instrumentation.runOnMainSync {
        Choreographer.getInstance().removeFrameCallback(callback)
        (popupField.get(activity) as? RecentVisitsPopup)?.dismissImmediately()
        activity.finish()
      }
      instrumentation.removeMonitor(monitor)
    }
  }
}
