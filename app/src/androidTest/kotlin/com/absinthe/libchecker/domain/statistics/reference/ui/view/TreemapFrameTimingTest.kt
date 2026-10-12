package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.os.Trace
import android.view.FrameMetrics
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Window
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run unchanged on both APKs; timing is reported rather than asserted against device-specific limits. */
@RunWith(AndroidJUnit4::class)
class TreemapFrameTimingTest {
  @Test
  fun zoomFramesAndAllocations() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val animatorField = LibReferenceTreemapView::class.java.getDeclaredField("zoomAnimator").apply { isAccessible = true }
    val metricsThread = HandlerThread("TreemapFrameMetrics").apply { start() }
    val handler = Handler(metricsThread.looper)
    try {
      ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
        lateinit var view: LibReferenceTreemapView
        lateinit var window: Window
        scenario.onActivity {
          view = LibReferenceTreemapView(it)
          it.setContentView(view)
          window = it.window
        }
        instrumentation.waitForIdleSync()
        for (count in listOf(80, 320)) {
          val references = (1..count).map { index ->
            LibReference("Library $index rendering test", null, (1..(index % 80 + 6)).map { "pkg$it" }.toSet(), NATIVE)
          }
          scenario.onActivity {
            view.submitReferences(references)
          }
          fun zoom(amount: Float) {
            scenario.onActivity {
              val properties = MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_MOUSE
              }
              val coords = MotionEvent.PointerCoords().apply {
                x = view.width * 0.65f
                y = view.height * 0.65f
                setAxisValue(MotionEvent.AXIS_VSCROLL, amount)
              }
              val now = SystemClock.uptimeMillis()
              val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1, arrayOf(properties), arrayOf(coords), 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0)
              try {
                assertTrue(view.onGenericMotionEvent(event))
                assertTrue((animatorField.get(view) as ValueAnimator).isRunning)
              } finally {
                event.recycle()
              }
            }
          }
          // Warm up both directions before collecting frame metrics.
          repeat(3) {
            zoom(1f)
            SystemClock.sleep(450)
            zoom(-1f)
            SystemClock.sleep(450)
          }
          val totals = mutableListOf<Long>()
          val draws = mutableListOf<Long>()
          var dropped = 0
          var missedDeadlines = 0
          val listener = Window.OnFrameMetricsAvailableListener { _, metrics, lost ->
            totals += metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            draws += metrics.getMetric(FrameMetrics.DRAW_DURATION)
            dropped += lost
            if (Build.VERSION.SDK_INT >= 31 && metrics.getMetric(FrameMetrics.TOTAL_DURATION) > metrics.getMetric(FrameMetrics.DEADLINE)) {
              missedDeadlines++
            }
          }
          scenario.onActivity { window.addOnFrameMetricsAvailableListener(listener, handler) }
          val allocatedBefore = Debug.getRuntimeStat("art.gc.bytes-allocated").toLong()
          val gcBefore = Debug.getRuntimeStat("art.gc.gc-count").toLong()
          Trace.beginSection("Treemap/$count/measure")
          try {
            repeat(10) {
              zoom(1f)
              SystemClock.sleep(450)
              zoom(-1f)
              SystemClock.sleep(450)
            }
          } finally {
            Trace.endSection()
          }
          val allocated = Debug.getRuntimeStat("art.gc.bytes-allocated").toLong() - allocatedBefore
          val gc = Debug.getRuntimeStat("art.gc.gc-count").toLong() - gcBefore
          scenario.onActivity { window.removeOnFrameMetricsAvailableListener(listener) }
          val drained = CountDownLatch(1)
          handler.post { drained.countDown() }
          assertTrue(drained.await(5, TimeUnit.SECONDS))
          assertTrue("No window frames collected", totals.size > 100)
          fun percentile(values: List<Long>, fraction: Double): Double = values.sorted()[((values.size - 1) * fraction).toInt()] / 1_000_000.0
          println(
            "TREEMAP_PERF cells=$count frames=${totals.size} dropped=$dropped " +
              "total_p50_ms=${percentile(totals, 0.50)} total_p95_ms=${percentile(totals, 0.95)} total_p99_ms=${percentile(totals, 0.99)} " +
              "draw_p95_ms=${percentile(draws, 0.95)} over_16ms=${totals.count { it > 16_666_667L }} missed_deadlines=$missedDeadlines allocated_bytes=$allocated gc=$gc"
          )
          zoom(1f)
          scenario.onActivity {
            val animator = animatorField.get(view) as ValueAnimator
            animator.pause()
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val bytes = ByteBuffer.allocate(bitmap.byteCount)
            fun snapshots(phase: String, animation: ValueAnimator, times: List<Long>) {
              animation.pause()
              for (time in times) {
                animation.currentPlayTime = time
                bitmap.eraseColor(0)
                view.draw(Canvas(bitmap))
                bytes.clear()
                bitmap.copyPixelsToBuffer(bytes)
                val digest = MessageDigest.getInstance("SHA-256").digest(bytes.array()).joinToString("") { "%02x".format(it) }
                println("TREEMAP_PIXELS cells=$count phase=$phase time=$time sha256=$digest")
              }
              animation.end()
            }
            snapshots("zoom", animator, listOf(0L, 40L, 120L, 220L, 329L, 330L))
            view.submitReferences(references.take(count / 2), animate = true)
            val filter = LibReferenceTreemapView::class.java.getDeclaredField("filterAnimator").apply { isAccessible = true }.get(view) as ValueAnimator
            snapshots("filter", filter, listOf(0L, 40L, 120L, 220L, 250L))
            bitmap.recycle()
          }
        }
      }
    } finally {
      metricsThread.quitSafely()
      metricsThread.join(5000)
    }
  }
}
