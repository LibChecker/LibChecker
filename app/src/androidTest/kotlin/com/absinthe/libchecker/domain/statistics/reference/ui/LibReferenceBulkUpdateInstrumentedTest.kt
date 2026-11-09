package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.LibReferenceAdapter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceBulkUpdateInstrumentedTest {
  @Test
  fun largeFilterReplacementDoesNotDispatchIndividualMoves() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val all = HashMap<String, LibReference>()
    val filtered = HashMap<String, LibReference>()
    repeat(10_000) { index ->
      val name = "com.example.library$index.Component"
      val reference = LibReference(name, null, setOf("app.one", "app.two"), NATIVE)
      all[name] = reference
      if (index < 5_000) filtered[name] = reference
    }
    val previous = all.values.toList()
    val next = filtered.values.toList()
    lateinit var adapter: LibReferenceAdapter
    val initialCommit = CountDownLatch(1)
    val committed = CountDownLatch(1)
    var moves = 0
    var updates = 0
    instrumentation.runOnMainSync {
      adapter = LibReferenceAdapter {}
      adapter.submitList(previous) { initialCommit.countDown() }
    }
    assertTrue("The initial list must commit before filtering", initialCommit.await(30, TimeUnit.SECONDS))
    instrumentation.runOnMainSync {
      adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
          updates++
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
          updates++
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
          moves += itemCount
        }
      })
      adapter.submitList(next) { committed.countDown() }
    }
    assertTrue("The filtered result must commit", committed.await(30, TimeUnit.SECONDS))
    instrumentation.runOnMainSync {
      assertEquals(next, adapter.items)
      assertTrue("Replacement must notify the list", updates > 0)
      assertEquals("Large replacements must not feed move operations into RecyclerView", 0, moves)
    }
  }

  @Test
  fun smallDemoCanStillAnimateMoves() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val initialCommit = CountDownLatch(1)
    val committed = CountDownLatch(1)
    lateinit var adapter: LibReferenceAdapter
    val previous = listOf(reference(0), reference(1), reference(2))
    var moves = 0
    instrumentation.runOnMainSync {
      adapter = LibReferenceAdapter(diffUpdates = true) {}
      adapter.submitList(previous) { initialCommit.countDown() }
    }
    assertTrue(initialCommit.await(10, TimeUnit.SECONDS))
    instrumentation.runOnMainSync {
      adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
          moves += itemCount
        }
      })
      adapter.submitList(previous.reversed()) { committed.countDown() }
    }
    assertTrue(committed.await(10, TimeUnit.SECONDS))
    instrumentation.runOnMainSync { assertTrue("Small demo updates must retain move animations", moves > 0) }
  }

  @Test
  fun largeReplacementLaysOutOnlyVisibleRows() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val all = (0 until 10_000).map(::reference)
    val filtered = all.take(5_000).reversed()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    lateinit var list: RecyclerView
    lateinit var adapter: LibReferenceAdapter
    try {
      instrumentation.runOnMainSync {
        adapter = LibReferenceAdapter {}
        list = RecyclerView(activity).apply {
          layoutManager = LinearLayoutManager(activity)
          this.adapter = adapter
        }
        activity.setContentView(list)
        adapter.submitList(all)
      }
      instrumentation.waitForIdleSync()
      for (next in listOf(filtered, all, emptyList(), filtered)) {
        instrumentation.runOnMainSync {
          val started = SystemClock.elapsedRealtimeNanos()
          adapter.submitList(next)
          list.measure(
            View.MeasureSpec.makeMeasureSpec(list.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(list.height, View.MeasureSpec.EXACTLY)
          )
          list.layout(list.left, list.top, list.right, list.bottom)
          val elapsedMs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000
          println("Reference replacement: ${next.size} rows, submit and layout ${elapsedMs}ms")
          assertEquals(next, adapter.items)
          assertTrue("Only visible rows should be laid out", list.childCount < 100)
          if (next.isNotEmpty()) assertTrue("The updated list must render rows", list.childCount > 0)
          assertTrue("Replacement must not block the main thread for seconds: ${elapsedMs}ms", elapsedMs < 1_000)
        }
        instrumentation.waitForIdleSync()
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }

  private fun reference(index: Int) = LibReference("com.example.library$index.Component", null, setOf("app.one", "app.two"), NATIVE)
}
