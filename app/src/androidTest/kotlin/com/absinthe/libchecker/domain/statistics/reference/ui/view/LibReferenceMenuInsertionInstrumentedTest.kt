package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Intent
import android.os.SystemClock
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceMenuBottomSheetState
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceMenuInsertionInstrumentedTest {
  @Test
  fun firstInsertionStaysAtTopWhileExpanding() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    lateinit var view: LibReferenceMenuBSDView
    lateinit var list: BottomSheetRecyclerView
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    try {
      instrumentation.runOnMainSync {
        view = LibReferenceMenuBSDView(activity)
        list = view.getChildAt(1) as BottomSheetRecyclerView
        activity.addContentView(view, ViewGroup.LayoutParams(1080, ViewGroup.LayoutParams.WRAP_CONTENT))
        view.bind(buildLibReferenceMenuBottomSheetState(LibReferenceOptions.SERVICES)) {}
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        assertTrue(view.isLaidOut)
        assertEquals(2, list.adapter!!.itemCount)
        view.bind(buildLibReferenceMenuBottomSheetState(LibReferenceOptions.SERVICES or LibReferenceOptions.NATIVE_LIBS)) {}
      }
      var sawExpansion = false
      val deadline = SystemClock.uptimeMillis() + 1000
      while (SystemClock.uptimeMillis() < deadline) {
        instrumentation.runOnMainSync {
          val manager = list.layoutManager as LinearLayoutManager
          val first = manager.findViewByPosition(0)
          assertTrue("Inserted first row must remain laid out", first != null)
          assertEquals("First row must not scroll above the viewport", list.paddingTop, manager.getDecoratedTop(first!!) - (first.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
          if (first.alpha > 0f && first.alpha < 1f) sawExpansion = true
        }
        SystemClock.sleep(16)
      }
      assertTrue("Insertion must animate rather than appear immediately", sawExpansion)
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
