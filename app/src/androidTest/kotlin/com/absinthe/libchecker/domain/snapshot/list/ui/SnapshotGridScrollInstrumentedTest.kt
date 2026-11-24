package com.absinthe.libchecker.domain.snapshot.list.ui

import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotGridScrollInstrumentedTest {
  @Test
  fun releasingDragAfterPageReattachmentBeforeLayoutPreservesBothColumns() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
      scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewpager).setCurrentItem(2, false) }
      lateinit var list: RecyclerView
      var ready = false
      val deadline = SystemClock.uptimeMillis() + 30_000
      while (!ready && SystemClock.uptimeMillis() < deadline) {
        scenario.onActivity { activity ->
          val fragment = activity.supportFragmentManager.fragments.filterIsInstance<SnapshotFragment>().firstOrNull()
          val candidate = fragment?.view?.findViewById<RecyclerView>(android.R.id.list)
          if (candidate?.layoutManager is StaggeredGridLayoutManager && candidate.isShown && candidate.childCount > 0) {
            list = candidate
            ready = true
          }
        }
        if (!ready) SystemClock.sleep(50)
      }
      assertTrue("Landscape snapshot list must be visible and laid out", ready)
      scenario.onActivity { activity ->
        val page = activity.supportFragmentManager.fragments.filterIsInstance<SnapshotFragment>().single().requireView()
        val host = page.parent as ViewGroup
        val index = host.indexOfChild(page)
        host.removeView(page)
        host.addView(page, index)
        assertTrue(list.isAttachedToWindow)
        val time = SystemClock.uptimeMillis()
        fun gesture(action: Int, elapsed: Long, y: Float, intercept: Boolean) {
          val event = MotionEvent.obtain(time, time + elapsed, action, 50f, y, 0)
          try {
            if (intercept) list.onInterceptTouchEvent(event) else list.onTouchEvent(event)
          } finally {
            event.recycle()
          }
        }
        gesture(MotionEvent.ACTION_DOWN, 0, 150f, true)
        gesture(MotionEvent.ACTION_MOVE, 100, 50f, true)
        assertEquals(RecyclerView.SCROLL_STATE_DRAGGING, list.scrollState)
        gesture(MotionEvent.ACTION_UP, 500, 50f, false)
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, list.scrollState)
      }
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      scenario.onActivity {
        val manager = list.layoutManager as StaggeredGridLayoutManager
        assertEquals(2, manager.spanCount)
        assertTrue(manager.findFirstVisibleItemPositions(null).all { it >= 0 })
      }
    }
  }
}
