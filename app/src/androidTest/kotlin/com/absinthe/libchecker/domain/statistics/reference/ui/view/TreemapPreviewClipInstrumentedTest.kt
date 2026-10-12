package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.customview.widget.ExploreByTouchHelper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreemapPreviewClipInstrumentedTest {
  @Test
  fun hardwarePreviewMatchesGlobalOutlineClip() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    fun field(name: String) = LibReferenceTreemapView::class.java.getDeclaredField(name).apply { isAccessible = true }
    val update = LibReferenceTreemapView::class.java.getDeclaredMethod("updateZoomPreview").apply { isAccessible = true }
    val settle = LibReferenceTreemapView::class.java.getDeclaredMethod("settleZoom", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).apply { isAccessible = true }
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      lateinit var activity: TrackActivity
      lateinit var view: LibReferenceTreemapView
      lateinit var container: FrameLayout
      var globalClip = false
      scenario.onActivity {
        activity = it
        view = LibReferenceTreemapView(it).apply { isFocusableInTouchMode = true }
        container = object : FrameLayout(it) {
          override fun dispatchDraw(canvas: Canvas) {
            val saved = canvas.save()
            if (globalClip) canvas.clipPath(field("outerPath").get(view) as Path)
            // Record both paths in one RenderNode so the reference does not add a parent clip mask.
            view.draw(canvas)
            canvas.restoreToCount(saved)
          }
        }
        container.setBackgroundColor(Color.WHITE)
        container.addView(view)
        it.setContentView(container)
      }
      instrumentation.waitForIdleSync()
      // PixelCopy includes the window's launch transition even while the main thread is idle.
      SystemClock.sleep(2000)
      for ((width, height, padding) in listOf(Triple(600, 900, 12), Triple(180, 320, 0), Triple(800, 480, 20))) {
        scenario.onActivity {
          view.layoutParams = FrameLayout.LayoutParams(width, height)
          view.setPadding(padding, padding, padding, padding)
        }
        instrumentation.waitForIdleSync()
        scenario.onActivity {
          assertEquals(width, view.width)
          assertEquals(height, view.height)
          view.submitReferences((1..80).map { LibReference("Library $it", null, (1..(it + 5)).map { n -> "pkg$n" }.toSet(), NATIVE) })
          field("zoomFocusX").setFloat(view, width * 0.65f)
          field("zoomFocusY").setFloat(view, height * 0.65f)
          assertTrue(view.isHardwareAccelerated)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = List(2) { IntArray(width * height) }
        try {
          for (zoom in listOf(1.5f, 0.6f)) {
            scenario.onActivity {
              field("zoom").setFloat(view, zoom)
              update.invoke(view)
              assertTrue(field("previewCells").get(view) != null)
            }
            for (progress in listOf(0f, 0.25f, 0.75f, 1f)) {
              scenario.onActivity {
                field("previewProgress").setFloat(view, progress)
                field("textTransitionProgress").setFloat(view, progress)
                field("animatedCell").setInt(view, 10)
                field("pressScale").setFloat(view, 0.96f)
                val accessibility = field("accessibility").get(view) as ExploreByTouchHelper
                accessibility.requestKeyboardFocusForVirtualView(0)
                assertEquals(0, accessibility.keyboardFocusedVirtualViewId)
                view.accessibilityNodeProvider!!.performAction(0, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
              }
              for (index in 0..1) {
                val rect = Rect()
                scenario.onActivity {
                  globalClip = index == 1
                  container.invalidate()
                  view.invalidate()
                  val location = IntArray(2)
                  view.getLocationInWindow(location)
                  rect.set(location[0], location[1], location[0] + width, location[1] + height)
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(80)
                val copied = CountDownLatch(1)
                var status = -1
                PixelCopy.request(activity.window, rect, bitmap, {
                  status = it
                  copied.countDown()
                }, Handler(Looper.getMainLooper()))
                assertTrue(copied.await(5, TimeUnit.SECONDS))
                assertEquals(PixelCopy.SUCCESS, status)
                bitmap.getPixels(pixels[index], 0, width, 0, 0, width, height)
              }
              var maxError = 0
              for (index in pixels[0].indices) {
                for (shift in 0..24 step 8) {
                  maxError = maxOf(maxError, abs((pixels[0][index] ushr shift and 255) - (pixels[1][index] ushr shift and 255)))
                }
              }
              assertTrue("$width x $height zoom=$zoom progress=$progress max channel error=$maxError", maxError <= 2)
            }
            scenario.onActivity {
              settle.invoke(view, false, true)
              (field("zoomAnimator").get(view) as ValueAnimator).end()
            }
          }
        } finally {
          bitmap.recycle()
        }
      }
    }
  }
}
