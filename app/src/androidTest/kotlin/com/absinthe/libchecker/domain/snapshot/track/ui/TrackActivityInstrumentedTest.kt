package com.absinthe.libchecker.domain.snapshot.track.ui

import android.content.Intent
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.snapshot.track.ui.adapter.TrackAdapter
import com.absinthe.libchecker.domain.snapshot.track.ui.view.TrackLoadingView
import com.absinthe.libchecker.view.app.EmptyListView
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackActivityInstrumentedTest {

  @Test
  fun stateHolderIsDisabledBeforeInitialListRowsAreInserted() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val intent = Intent(instrumentation.targetContext, TrackActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val monitor = instrumentation.addMonitor(TrackActivity::class.java.name, null, false)
    instrumentation.targetContext.startActivity(intent)
    val activity = instrumentation.waitForMonitorWithTimeout(monitor, 2_000L) as TrackActivity

    val commitObserved = AtomicBoolean(false)
    val stateHolderEnabledAtInsert = AtomicBoolean(true)
    var adapter: TrackAdapter? = null
    var observer: RecyclerView.AdapterDataObserver? = null

    try {
      instrumentation.runOnMainSync {
        val trackAdapter = activity.findViewById<RecyclerView>(android.R.id.list).adapter as TrackAdapter
        adapter = trackAdapter
        assertTrue("The initial adapter state is not loading", trackAdapter.stateView is TrackLoadingView)
        assertTrue("The initial state holder is not enabled", trackAdapter.isStateViewEnable)
        val commitObserver = object : RecyclerView.AdapterDataObserver() {
          override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (itemCount <= 0 || trackAdapter.data.isEmpty()) return
            stateHolderEnabledAtInsert.set(trackAdapter.isStateViewEnable)
            commitObserved.set(true)
          }
        }
        observer = commitObserver
        trackAdapter.registerAdapterDataObserver(commitObserver)
      }

      val deadline = SystemClock.uptimeMillis() + 10_000L
      while (!commitObserved.get() && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(16L)
      }
    } finally {
      instrumentation.runOnMainSync {
        val trackAdapter = adapter
        val commitObserver = observer
        if (trackAdapter != null && commitObserver != null) {
          trackAdapter.unregisterAdapterDataObserver(commitObserver)
        }
        activity.finish()
      }
      instrumentation.removeMonitor(monitor)
    }

    assertTrue("The initial track list was never committed", commitObserved.get())
    assertFalse("The loading holder was still enabled while list rows were inserted", stateHolderEnabledAtInsert.get())
  }

  @Test
  fun emptyHolderIsDisabledBeforeFilteredRowsAreRestored() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val intent = Intent(instrumentation.targetContext, TrackActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val monitor = instrumentation.addMonitor(TrackActivity::class.java.name, null, false)
    instrumentation.targetContext.startActivity(intent)
    val activity = instrumentation.waitForMonitorWithTimeout(monitor, 2_000L) as TrackActivity

    val rowsRestored = AtomicBoolean(false)
    val stateHolderEnabledAtInsert = AtomicBoolean(true)
    var adapter: TrackAdapter? = null
    var observer: RecyclerView.AdapterDataObserver? = null

    try {
      assertTrue(
        "The initial track list did not load",
        waitUntil(instrumentation) {
          val trackAdapter = activity.findViewById<RecyclerView>(android.R.id.list).adapter as TrackAdapter
          adapter = trackAdapter
          trackAdapter.data.isNotEmpty()
        }
      )

      instrumentation.runOnMainSync {
        activity.onQueryTextChange("__codex_no_track_item_should_match_8f31c__")
      }
      assertTrue(
        "Filtering did not show the empty holder",
        waitUntil(instrumentation) {
          val trackAdapter = checkNotNull(adapter)
          trackAdapter.data.isEmpty() &&
            trackAdapter.stateView is EmptyListView &&
            trackAdapter.isStateViewEnable
        }
      )

      instrumentation.runOnMainSync {
        val trackAdapter = checkNotNull(adapter)
        val restoreObserver = object : RecyclerView.AdapterDataObserver() {
          override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (itemCount <= 0 || trackAdapter.data.isEmpty()) return
            stateHolderEnabledAtInsert.set(trackAdapter.isStateViewEnable)
            rowsRestored.set(true)
          }
        }
        observer = restoreObserver
        trackAdapter.registerAdapterDataObserver(restoreObserver)
        activity.onQueryTextChange("")
      }

      val deadline = SystemClock.uptimeMillis() + 10_000L
      while (!rowsRestored.get() && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(16L)
      }
    } finally {
      instrumentation.runOnMainSync {
        val trackAdapter = adapter
        val restoreObserver = observer
        if (trackAdapter != null && restoreObserver != null) {
          trackAdapter.unregisterAdapterDataObserver(restoreObserver)
        }
        activity.finish()
      }
      instrumentation.removeMonitor(monitor)
    }

    assertTrue("The filtered track rows were never restored", rowsRestored.get())
    assertFalse("The empty holder was still enabled while list rows were inserted", stateHolderEnabledAtInsert.get())
  }

  private fun waitUntil(
    instrumentation: android.app.Instrumentation,
    condition: () -> Boolean
  ): Boolean {
    val conditionMet = AtomicBoolean(false)
    val deadline = SystemClock.uptimeMillis() + 10_000L
    while (!conditionMet.get() && SystemClock.uptimeMillis() < deadline) {
      instrumentation.runOnMainSync {
        conditionMet.set(condition())
      }
      if (!conditionMet.get()) {
        SystemClock.sleep(16L)
      }
    }
    return conditionMet.get()
  }
}
