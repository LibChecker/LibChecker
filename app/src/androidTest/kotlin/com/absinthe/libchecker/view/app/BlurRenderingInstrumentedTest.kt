package com.absinthe.libchecker.view.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.domain.statistics.reference.ui.LibReferenceActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationBarView
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class BlurRenderingInstrumentedTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()

  @Test
  fun capturedContentReusesDisplayListAndRefreshesAfterInvalidation() = withActivity { activity ->
    lateinit var container: BlurCoordinatorLayout
    lateinit var content: CountingView
    instrumentation.runOnMainSync {
      container = BlurCoordinatorLayout(activity, contentViewId = R.id.vf_container)
      content = CountingView(activity).apply { id = R.id.vf_container }
      container.addView(content, CoordinatorLayout.LayoutParams(400, 600))
      container.setBlurEnabled(true)
      activity.setContentView(container)
    }
    settle()
    assertTrue(content.drawCount > 0)
    val initialDraws = content.drawCount
    repeat(10) {
      instrumentation.runOnMainSync { container.invalidate() }
      settle()
    }
    val repeatedDraws = content.drawCount - initialDraws
    instrumentation.sendStatus(0, Bundle().apply { putString("blurRedraws", "$repeatedDraws") })
    assertEquals("Parent-only redraws must reuse the child's display list", 0, repeatedDraws)
    instrumentation.runOnMainSync {
      content.color = Color.GREEN
      content.invalidate()
    }
    settle()
    assertTrue("Child invalidation must update captured content", content.drawCount > initialDraws)
    val updatedDraws = content.drawCount
    instrumentation.runOnMainSync { content.layoutParams = content.layoutParams.apply { width = 500 } }
    settle()
    assertTrue("Resize must redraw the child", content.drawCount > updatedDraws)
  }

  @Test
  fun capturedPixelsRefreshThroughBlurAndViewTransforms() = withActivity { activity ->
    lateinit var container: BlurCoordinatorLayout
    lateinit var content: CountingView
    instrumentation.runOnMainSync {
      container = BlurCoordinatorLayout(activity, contentViewId = R.id.vf_container)
      content = CountingView(activity).apply { id = R.id.vf_container }
      container.addView(content, CoordinatorLayout.LayoutParams(400, 600))
      container.addView(
        AppBarLayout(activity).apply { id = R.id.appbar },
        CoordinatorLayout.LayoutParams(400, 150)
      )
      container.setBlurEnabled(true)
      activity.setContentView(container)
    }
    settle()
    assertPixel(activity, container, 200, 75, Color.RED)
    assertPixel(activity, container, 200, 250, Color.RED)
    instrumentation.runOnMainSync {
      content.color = Color.GREEN
      content.invalidate()
    }
    settle()
    assertPixel(activity, container, 200, 75, Color.GREEN)
    assertPixel(activity, container, 200, 250, Color.GREEN)
    instrumentation.runOnMainSync { content.alpha = 0f }
    settle()
    val backdrop = pixel(activity, container, 200, 250)
    assertTrue("Alpha zero must hide captured content", backdrop != Color.GREEN)
    instrumentation.runOnMainSync {
      content.alpha = 1f
      content.translationX = 100f
    }
    settle()
    assertPixel(activity, container, 50, 250, backdrop)
    assertPixel(activity, container, 200, 250, Color.GREEN)
    instrumentation.runOnMainSync {
      content.translationX = 0f
      content.pivotX = 0f
      content.scaleX = 0.5f
    }
    settle()
    assertPixel(activity, container, 300, 250, backdrop)
    assertPixel(activity, container, 100, 250, Color.GREEN)
    instrumentation.runOnMainSync { content.visibility = View.INVISIBLE }
    settle()
    assertPixel(activity, container, 100, 250, backdrop)
  }

  @Test
  fun pageTransitionsKeepBlurVisibleInBothThemes() {
    val originalNightMode = AppCompatDelegate.getDefaultNightMode()
    try {
      for ((theme, mode) in listOf("light" to AppCompatDelegate.MODE_NIGHT_NO, "dark" to AppCompatDelegate.MODE_NIGHT_YES)) {
        instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(mode) }
        withActivity { activity ->
          instrumentation.runOnMainSync { activity.setBlurDesignEnabled(true) }
          SystemClock.sleep(1500)
          val nav = activity.findViewById<NavigationBarView>(R.id.nav_view)
          for ((page, id) in listOf("home" to R.id.navigation_app_list, "settings" to R.id.navigation_settings, "home-return" to R.id.navigation_app_list)) {
            instrumentation.runOnMainSync { nav.selectedItemId = id }
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync {
              assertEquals(id, nav.selectedItemId)
              assertTrue(activity.findViewById<BlurCoordinatorLayout>(R.id.container).blurEnabled)
              assertEquals(1f, activity.findViewById<View>(R.id.viewpager).alpha, 0.001f)
            }
            instrumentation.sendStatus(0, Bundle().apply { putString("blurVisual", "$theme-$page") })
            SystemClock.sleep(2000)
          }
        }
      }
    } finally {
      instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(originalNightMode) }
    }
  }

  private fun assertPixel(activity: MainActivity, view: View, x: Int, y: Int, expected: Int) {
    val actual = pixel(activity, view, x, y)
    for (shift in listOf(0, 8, 16)) {
      assertTrue(
        "Pixel at $x,$y expected ${expected.toUInt().toString(16)}, got ${actual.toUInt().toString(16)}",
        kotlin.math.abs(((expected shr shift) and 255) - ((actual shr shift) and 255)) <= 3
      )
    }
  }

  private fun pixel(activity: MainActivity, view: View, x: Int, y: Int): Int {
    val ready = CountDownLatch(1)
    var result = -1
    lateinit var bitmap: Bitmap
    val location = IntArray(2)
    instrumentation.runOnMainSync {
      val decor = activity.window.decorView
      bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
      view.getLocationInWindow(location)
      PixelCopy.request(activity.window, bitmap, {
        result = it
        ready.countDown()
      }, Handler(android.os.Looper.getMainLooper()))
    }
    assertTrue("PixelCopy timed out", ready.await(5, TimeUnit.SECONDS))
    assertEquals(PixelCopy.SUCCESS, result)
    return bitmap.getPixel(location[0] + x, location[1] + y).also { bitmap.recycle() }
  }

  /** Outputs raw per-frame nanoseconds without writing artifacts outside /data/local/tmp. */
  @Test
  fun measureHomeScrollAndIdle() = withActivity { activity ->
    measureScrollAndIdle(activity, "home") { activity.setBlurDesignEnabled(it) }
  }

  @Test
  fun measureLibraryReferenceScrollAndIdle() {
    val monitor = instrumentation.addMonitor(LibReferenceActivity::class.java.name, null, false)
    val originalBlur = GlobalValues.isBlurDesign
    var activity: LibReferenceActivity? = null
    try {
      GlobalValues.isBlurDesign = true
      instrumentation.targetContext.startActivity(
        Intent(instrumentation.targetContext, LibReferenceActivity::class.java)
          .putExtra(EXTRA_REF_NAME, "android.permission.INTERNET")
          .putExtra(EXTRA_REF_TYPE, PERMISSION)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
      activity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as LibReferenceActivity
      val referenceActivity = activity
      measureScrollAndIdle(referenceActivity, "reference") {
        referenceActivity.findViewById<BlurCoordinatorLayout>(R.id.container).setBlurEnabled(it)
      }
    } finally {
      instrumentation.runOnMainSync {
        GlobalValues.isBlurDesign = originalBlur
        activity?.finish()
      }
      instrumentation.removeMonitor(monitor)
    }
  }

  private fun measureScrollAndIdle(activity: Activity, screen: String, setBlur: (Boolean) -> Unit) {
    val collectorThread = HandlerThread("BlurFrameMetrics").apply { start() }
    val frames = Collections.synchronizedList(mutableListOf<LongArray>())
    val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
      if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 0L) {
        frames.add(
          longArrayOf(
            metrics.getMetric(FrameMetrics.TOTAL_DURATION),
            metrics.getMetric(FrameMetrics.DRAW_DURATION),
            metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION),
            metrics.getMetric(FrameMetrics.GPU_DURATION),
            dropped.toLong(),
            metrics.getMetric(FrameMetrics.DEADLINE)
          )
        )
      }
    }
    activity.window.addOnFrameMetricsAvailableListener(listener, Handler(collectorThread.looper))
    try {
      var list: RecyclerView? = null
      val deadline = SystemClock.uptimeMillis() + 20_000L
      while (SystemClock.uptimeMillis() < deadline) {
        var ready = false
        instrumentation.runOnMainSync {
          val found = activity.findViewById<RecyclerView>(android.R.id.list)
          if (found != null && (found.adapter?.itemCount ?: 0) > 15) {
            list = found
            ready = true
          }
        }
        if (ready) break
        SystemClock.sleep(100)
      }
      val appList = checkNotNull(list) { "$screen list not populated" }
      for (enabled in listOf(false, true)) {
        instrumentation.runOnMainSync {
          setBlur(enabled)
          appList.scrollToPosition(0)
        }
        SystemClock.sleep(1500)
        repeat(3) { run ->
          frames.clear()
          repeat(8) { step ->
            instrumentation.runOnMainSync {
              appList.smoothScrollBy(0, if (step % 2 == 0) 900 else -900, null, 400)
            }
            SystemClock.sleep(600)
          }
          report("$screen-scroll-$enabled-$run", frames)
        }
        frames.clear()
        SystemClock.sleep(2000)
        report("$screen-idle-$enabled", frames)
      }
    } finally {
      activity.window.removeOnFrameMetricsAvailableListener(listener)
      collectorThread.quitSafely()
    }
  }

  private fun report(name: String, frames: MutableList<LongArray>) {
    val snapshot = synchronized(frames) { frames.toList() }
    instrumentation.sendStatus(
      0,
      Bundle().apply { putString("blurFrames", name + ":" + snapshot.joinToString(";") { it.joinToString(",") }) }
    )
  }

  private fun withActivity(block: (MainActivity) -> Unit) {
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    val originalBlur = GlobalValues.isBlurDesign
    var activity: MainActivity? = null
    try {
      instrumentation.targetContext.startActivity(
        Intent(instrumentation.targetContext, MainActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      )
      activity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as MainActivity
      settle()
      block(activity)
    } finally {
      instrumentation.runOnMainSync {
        GlobalValues.isBlurDesign = originalBlur
        activity?.finish()
      }
      instrumentation.removeMonitor(monitor)
    }
  }

  private fun settle() {
    instrumentation.waitForIdleSync()
    SystemClock.sleep(150)
  }

  private class CountingView(context: Context) : View(context) {
    var drawCount = 0
    var color = Color.RED

    override fun onDraw(canvas: Canvas) {
      drawCount++
      canvas.drawColor(color)
    }
  }
}
