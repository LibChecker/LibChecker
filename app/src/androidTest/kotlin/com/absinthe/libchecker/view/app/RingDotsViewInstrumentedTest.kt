package com.absinthe.libchecker.view.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RingDotsViewInstrumentedTest {
  @Test
  fun nextIconExpandsWhilePreviousIconShrinksAndBothAreReleased() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val context = ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().targetContext, R.style.AppTheme)
      val size = (200 * context.resources.displayMetrics.density).toInt()
      val view = RingDotsView(context).apply { layout(0, 0, size, size) }
      val outgoing = createBitmap(40, 40).apply { eraseColor(Color.RED) }
      val incoming = createBitmap(40, 40).apply { eraseColor(Color.GREEN) }
      view.setField("currentHighlightBitmap", outgoing)
      view.setField("pendingHighlightBitmap", incoming)
      view.setField("highlightIndex", 0)
      view.setField("highlightStartedAt", 1000L)
      val update = view.javaClass.getDeclaredMethod("updateHighlight", Long::class.javaPrimitiveType).apply { isAccessible = true }
      update.invoke(view, 2505L)
      assertTrue(view.field("outgoingHighlightBitmap") === outgoing)
      assertTrue(view.field("currentHighlightBitmap") === incoming)
      update.invoke(view, 2605L)
      val shrinking = view.field("outgoingHighlightProgress") as Float
      val growing = view.field("highlightProgress") as Float
      assertTrue("Outgoing icon must still be visible", shrinking > 0f && shrinking < 1f)
      assertTrue("Incoming icon must already be growing", growing > 0f && growing < 1f)
      val frame = createBitmap(size, size)
      view.draw(Canvas(frame))
      update.invoke(view, 2655L)
      assertTrue((view.field("outgoingHighlightProgress") as Float) < shrinking)
      assertTrue((view.field("highlightProgress") as Float) > growing)
      assertTrue(!outgoing.isRecycled)
      update.invoke(view, 3100L)
      assertTrue(outgoing.isRecycled)
      assertTrue(!incoming.isRecycled)
      view.setAppIconHighlightProvider { null }
      assertTrue(incoming.isRecycled)
      assertTrue(view.field("outgoingHighlightBitmap") == null)
      frame.recycle()
    }
  }

  @Test
  fun neighborDotIsPushedAsideWithoutFadingAndReturnsToItsOrbit() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val density = context.resources.displayMetrics.density
      val size = (200 * density).toInt()
      val view = RingDotsView(context).apply { layout(0, 0, size, size) }
      val icon = createBitmap(40, 40).apply { eraseColor(Color.WHITE) }
      view.setField("currentHighlightBitmap", icon)
      view.setField("highlightIndex", 0)
      val baseline = createBitmap(size, size)
      view.draw(Canvas(baseline))
      val radius = (view.field("ringRadii") as FloatArray).last()
      val angle = Math.toRadians(15.0)
      val color = baseline.getPixel(
        (size / 2f + cos(angle) * radius).roundToInt(),
        (size / 2f + sin(angle) * radius).roundToInt()
      )
      val origin = baseline.centerOfColor(color)
      val frame = createBitmap(size, size)
      for (progress in listOf(0.35f, 0.65f, 1f, 0.65f, 0.35f, 0f)) {
        frame.eraseColor(Color.TRANSPARENT)
        view.setField("highlightProgress", progress)
        view.draw(Canvas(frame))
        val center = frame.centerOfColor(color)
        if (progress == 1f) {
          assertTrue("Neighbor dot did not move aside", hypot(center.first - origin.first, center.second - origin.second) > 2f * density)
        }
      }
      assertTrue("Dots did not return to their original orbit", frame.sameAs(baseline))
      frame.recycle()
      baseline.recycle()
      icon.recycle()
    }
  }

  @Test
  fun highlightFitsEveryPositionAndPreservesWideIcons() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val view = RingDotsView(context)
      val icon = createBitmap(80, 40).apply { eraseColor(Color.WHITE) }
      view.setField("currentHighlightBitmap", icon)
      view.setField("highlightProgress", 1f)
      for (sizeDp in listOf(120, 200)) {
        val size = (sizeDp * context.resources.displayMetrics.density).toInt()
        view.layout(0, 0, size, size)
        val frame = createBitmap(size + 2, size + 2)
        val canvas = Canvas(frame).apply { translate(1f, 1f) }
        for (index in 0 until 24) {
          frame.eraseColor(Color.TRANSPARENT)
          view.setField("highlightIndex", index)
          view.drawHighlight(canvas)
          val bounds = view.field("highlightDst") as RectF
          assertTrue(
            "Icon clipped at $sizeDp dp, position $index: $bounds",
            bounds.left >= 0f && bounds.top >= 0f && bounds.right <= size && bounds.bottom <= size
          )
          assertEquals("Wide icon was stretched", 2f, bounds.width() / bounds.height(), 0.01f)
          for (edge in 0 until frame.width) {
            assertEquals(
              "Highlight halo exceeds view bounds",
              Color.TRANSPARENT,
              frame.getPixel(edge, 0) or frame.getPixel(edge, size + 1) or
                frame.getPixel(0, edge) or frame.getPixel(size + 1, edge)
            )
          }
        }
        frame.recycle()
      }
      icon.recycle()
    }
  }

  @Test
  fun highlightFadesContinuouslyFromTheDot() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val view = RingDotsView(context)
      val size = (200 * context.resources.displayMetrics.density).toInt()
      view.layout(0, 0, size, size)
      val icon = createBitmap(40, 40).apply { eraseColor(Color.WHITE) }
      val frame = createBitmap(size, size)
      view.setField("currentHighlightBitmap", icon)
      view.setField("highlightIndex", 3)
      val pixels = IntArray(size * size)
      var previousAlpha = 0
      for (progress in listOf(0.01f, 0.12f, 0.13f, 0.5f, 1f)) {
        frame.eraseColor(Color.TRANSPARENT)
        view.setField("highlightProgress", progress)
        view.drawHighlight(Canvas(frame))
        frame.getPixels(pixels, 0, size, 0, 0, size, size)
        val alpha = pixels.maxOf(Color::alpha)
        assertTrue("Highlight popped in at $progress: $alpha", alpha <= (255 * progress * 2.2f).toInt())
        assertTrue("Highlight did not fade in at $progress", alpha > previousAlpha)
        previousAlpha = alpha
      }
      frame.recycle()
      icon.recycle()
    }
  }

  private fun RingDotsView.setField(name: String, value: Any) {
    javaClass.getDeclaredField(name).apply { isAccessible = true }.set(this, value)
  }

  private fun RingDotsView.field(name: String): Any? = javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)

  private fun Bitmap.centerOfColor(color: Int): Pair<Float, Float> {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var x = 0f
    var y = 0f
    var count = 0
    pixels.forEachIndexed { index, pixel ->
      if (pixel == color) {
        x += index % width
        y += index / width
        count++
      }
    }
    assertTrue("Neighbor dot faded instead of moving", count > 0)
    return x / count to y / count
  }

  private fun RingDotsView.drawHighlight(canvas: Canvas) {
    javaClass.getDeclaredMethod("drawHighlight", Canvas::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
      .apply { isAccessible = true }
      .invoke(this, canvas, width / 2f, height / 2f)
  }
}
