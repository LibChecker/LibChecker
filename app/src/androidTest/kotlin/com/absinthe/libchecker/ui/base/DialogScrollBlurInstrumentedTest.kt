package com.absinthe.libchecker.ui.base

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class DialogScrollBlurInstrumentedTest {
  @Test
  fun onlyScrollableEdgesFadeAndAnOpenDialogTracksThePreference() {
    val original = GlobalValues.isBlurDesign
    try {
      ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
        lateinit var dialog: AlertDialog
        lateinit var scroll: ScrollView
        var viewportHeight = 0
        scenario.onActivity { activity ->
          viewportHeight = activity.resources.displayMetrics.heightPixels / 2
          GlobalValues.isBlurDesign = true
          scroll = ScrollView(activity).apply {
            addView(
              View(activity).apply {
                minimumHeight = viewportHeight * 3
                setBackgroundColor(Color.RED)
              }
            )
          }
          val body = android.widget.FrameLayout(activity).apply {
            addView(scroll, ViewGroup.LayoutParams.MATCH_PARENT, viewportHeight)
          }
          dialog = BaseAlertDialogBuilder(activity).setTitle("Scroll edges").setView(body).show()
        }
        fun checkEdges(top: Boolean, bottom: Boolean): Int {
          val location = IntArray(2)
          var width = 0
          var height = 0
          scenario.onActivity {
            scroll.getLocationInWindow(location)
            width = scroll.width
            height = scroll.height
          }
          val deadline = SystemClock.elapsedRealtime() + 4000
          do {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val result = ArrayBlockingQueue<Int>(1)
            val rect = Rect(location[0], location[1], location[0] + width, location[1] + height)
            PixelCopy.request(requireNotNull(dialog.window), rect, bitmap, { result.add(it) }, Handler(Looper.getMainLooper()))
            assertEquals(PixelCopy.SUCCESS, result.poll(2, TimeUnit.SECONDS))
            fun isRed(y: Int): Boolean {
              val color = bitmap.getPixel(width / 2, y)
              return Color.red(color) > 225 && Color.green(color) < 30 && Color.blue(color) < 30
            }
            val actual = listOf(isRed(2), isRed(height / 2), isRed(height - 3))
            val bottomColor = bitmap.getPixel(width / 2, height - 3)
            val samples = listOf(2, height / 2, height - 3).map { Integer.toHexString(bitmap.getPixel(width / 2, it)) }
            bitmap.recycle()
            val expected = listOf(!top, true, !bottom)
            if (expected == actual || SystemClock.elapsedRealtime() >= deadline) {
              assertEquals("Viewport $rect, sampled $samples", expected, actual)
              return bottomColor
            }
            SystemClock.sleep(50)
          } while (true)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val restingEdge = checkEdges(top = false, bottom = true)
        val downTime = SystemClock.uptimeMillis()
        fun touch(action: Int, y: Float) {
          scenario.onActivity {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, scroll.width / 2f, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            scroll.dispatchTouchEvent(event)
            event.recycle()
          }
        }
        touch(MotionEvent.ACTION_DOWN, 12f)
        touch(MotionEvent.ACTION_MOVE, 60f)
        touch(MotionEvent.ACTION_MOVE, viewportHeight * 0.9f)
        try {
          val committed = CountDownLatch(1)
          scenario.onActivity {
            scroll.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
            scroll.invalidate()
          }
          assertTrue(committed.await(2, TimeUnit.SECONDS))
          val stretchedEdge = checkEdges(top = false, bottom = true)
          // Red content pulled into the fixed bottom edge would lower its green channel.
          assertEquals(Color.green(restingEdge).toDouble(), Color.green(stretchedEdge).toDouble(), 4.0)
        } finally {
          touch(MotionEvent.ACTION_CANCEL, viewportHeight * 0.9f)
        }
        scenario.onActivity { scroll.scrollTo(0, 500) }
        checkEdges(top = true, bottom = true)
        scenario.onActivity { GlobalValues.isBlurDesign = false }
        checkEdges(top = false, bottom = false)
        scenario.onActivity {
          GlobalValues.isBlurDesign = true
          scroll.fullScroll(View.FOCUS_DOWN)
        }
        checkEdges(top = true, bottom = false)
        scenario.onActivity {
          scroll.getChildAt(0).minimumHeight = viewportHeight
        }
        checkEdges(top = false, bottom = false)
        scenario.onActivity { dialog.dismiss() }
      }
    } finally {
      GlobalValues.isBlurDesign = original
    }
  }
}
