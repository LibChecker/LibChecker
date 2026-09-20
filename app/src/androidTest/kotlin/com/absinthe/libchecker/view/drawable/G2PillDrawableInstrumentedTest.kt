package com.absinthe.libchecker.view.drawable

import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class G2PillDrawableInstrumentedTest {

  @Test
  fun sharedPathsStayFiniteAndWithinBoundsForAllCallerGeometries() {
    val path = Path()
    val bounds = RectF()
    val position = FloatArray(2)
    for ((width, height) in listOf(240f to 48f, 48f to 240f, 48f to 48f, 320f to 180f, 16f to 48f, 2f to 80f)) {
      for (radius in listOf(0f, 2f, 8f, 18f, height / 2f, height)) {
        for (smoothing in listOf(null, 0f, 0.5f, 1f)) {
          for (rightRadius in listOf(radius, 8f)) {
            path.setG2Shape(1f, 1f, width + 1f, height + 1f, radius, cornerSmoothing = smoothing, rightCornerRadius = rightRadius)
            val measure = PathMeasure(path, true)
            assertTrue(measure.length.isFinite() && measure.length > 0f)
            for (sample in 0..100) {
              assertTrue(measure.getPosTan(measure.length * sample / 100f, position, null))
              assertTrue(position.all { it.isFinite() })
              assertTrue(position[0] in 0.99f..(width + 1.01f))
              assertTrue(position[1] in 0.99f..(height + 1.01f))
            }
          }
        }
      }
    }
    for (anchor in listOf(-100f, 0f, 160f, 320f, 500f)) {
      path.setG2Shape(0f, 0f, 320f, 180f, 18f, anchor, 32.4f, 12f)
      path.computeBounds(bounds, true)
      assertTrue(bounds.left >= 0f && bounds.right <= 320f)
      assertTrue(bounds.top == 0f && bounds.bottom == 192f)
    }
    path.setG2Shape(0f, 0f, 0f, 10f, 8f)
    assertTrue(path.isEmpty)
  }

  @Test
  fun getOutlineSupportsCurrentDevice() {
    val drawable = G2PillDrawable(fillColor = Color.BLACK)
    drawable.setBounds(0, 0, 240, 48)
    val outline = Outline()

    drawable.getOutline(outline)

    assertTrue(outline.canClip())
  }

  @Test
  fun invalidConvexPathUsesFallback() {
    var fallbackCalled = false

    setConvexPathOrFallback(
      setConvexPath = { throw IllegalArgumentException("path must be convex") },
      setFallback = { fallbackCalled = true }
    )

    assertTrue(fallbackCalled)
  }
}
