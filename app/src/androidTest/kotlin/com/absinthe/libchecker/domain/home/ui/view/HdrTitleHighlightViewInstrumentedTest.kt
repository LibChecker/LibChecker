package com.absinthe.libchecker.domain.home.ui.view

import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.home.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HdrTitleHighlightViewInstrumentedTest {

  @Test
  fun hdrHighlightIsDisabledByDefault() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val view = HdrTitleHighlightView(context)
    var requestedHeadroom = 0f
    view.setHdrHeadroomChangedListener { requestedHeadroom = it }

    assertFalse(view.hdrHighlightEnabled)
    assertEquals(1f, requestedHeadroom, 0f)
    view.setHdrHighlightEnabled(true)
    assertTrue(view.hdrHighlightEnabled)
    assertEquals(0f, view.hdrHighlightProgress, 0f)
    assertEquals(1f, requestedHeadroom, 0f)
    view.setHdrHighlightEnabled(false, animate = false)
    assertFalse(view.hdrHighlightEnabled)
    assertEquals(0f, view.hdrHighlightProgress, 0f)
    assertEquals(1f, requestedHeadroom, 0f)
  }

  @Test
  fun detachedTitleResetsHeadroomAndRestoresItsTargetWhenReattached() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    instrumentation.targetContext.startActivity(
      Intent(instrumentation.targetContext, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
    )
    val activity = checkNotNull(
      instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
    ) as MainActivity
    instrumentation.waitForIdleSync()

    try {
      instrumentation.runOnMainSync {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val view = HdrTitleHighlightView(activity)
        var requestedHeadroom = 0f
        view.setHdrHeadroomChangedListener { requestedHeadroom = it }
        root.addView(view, ViewGroup.LayoutParams(200, 80))
        view.setHdrHighlightEnabled(true, animate = false)
        assertEquals(1f, view.hdrHighlightProgress, 0f)
        assertEquals(3f, requestedHeadroom, 0f)

        root.removeView(view)
        assertEquals(0f, view.hdrHighlightProgress, 0f)
        assertEquals(1f, requestedHeadroom, 0f)

        root.addView(view, ViewGroup.LayoutParams(200, 80))
        assertEquals(1f, view.hdrHighlightProgress, 0f)
        assertEquals(3f, requestedHeadroom, 0f)
        root.removeView(view)
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
      instrumentation.removeMonitor(monitor)
    }
  }

  @Test
  fun hdrContentBoostInterpolatesFromSdrToPeak() {
    assertEquals(1f, hdrContentBoost(0f), 0f)
    assertEquals(2f, hdrContentBoost(0.5f), 0f)
    assertEquals(3f, hdrContentBoost(1f), 0f)
  }

  @Test
  fun gainmapBoostsEntireTitle() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val gainmap = HdrTitleHighlightView(context).createUniformGainmap(WIDTH, HEIGHT)
    val contents = gainmap.gainmapContents

    val left = Color.red(contents.getPixel(1, HEIGHT - 2))
    val center = Color.red(contents.getPixel(WIDTH / 2, HEIGHT / 2))
    val right = Color.red(contents.getPixel(WIDTH - 2, 1))

    assertTrue("Left edge must reach peak gain", left >= PEAK_MIN)
    assertTrue("Center must reach peak gain", center >= PEAK_MIN)
    assertTrue("Right edge must reach peak gain", right >= PEAK_MIN)
  }

  private companion object {
    const val WIDTH = 200
    const val HEIGHT = 40
    const val PEAK_MIN = 247
  }
}
