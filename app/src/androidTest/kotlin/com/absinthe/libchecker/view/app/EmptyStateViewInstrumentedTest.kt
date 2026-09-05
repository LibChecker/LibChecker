package com.absinthe.libchecker.view.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotEmptyView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyStateViewInstrumentedTest {

  @Test
  fun copyWrapsAndRemainsReachableInNarrowAndShortContainers() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
        for (fontScale in listOf(1f, 2f)) {
          val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
            this.fontScale = fontScale
          }
          val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(configuration), R.style.AppTheme)
          val density = context.resources.displayMetrics.density
          for (view in listOf(EmptyListView(context), SnapshotEmptyView(context))) {
            view.text.text = "没有找到符合当前条件的应用，请尝试其他搜索条件。 No matching applications were found."
            for (height in listOf(160, 640)) {
              val widthPx = (280 * density).toInt()
              val heightPx = (height * density).toInt()
              view.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
              )
              view.layout(0, 0, widthPx, heightPx)
              assertEquals(widthPx, view.measuredWidth)
              assertEquals(heightPx, view.measuredHeight)
              val content = view.getChildAt(0) as LinearLayout
              val illustration = content.getChildAt(0)
              assertTrue(illustration.top >= content.paddingTop)
              assertTrue(view.text.top > illustration.bottom)
              assertTrue(view.text.left >= content.paddingLeft)
              assertTrue(view.text.right <= widthPx - content.paddingRight)
              assertTrue(view.text.bottom <= content.height - content.paddingBottom)
              val layout = view.text.layout
              assertTrue(layout.lineCount > 1)
              assertEquals(view.text.length(), layout.getLineEnd(layout.lineCount - 1))
              for (line in 0 until layout.lineCount) assertEquals(0, layout.getEllipsisCount(line))
              view.scrollTo(0, content.height)
              assertTrue(view.text.bottom - view.scrollY <= heightPx)
              view.scrollTo(0, 0)
            }
          }
        }
      }
    }
  }

  @Test
  fun illustrationsHaveComparableVisibleWidthsAndCompactStatesWrapContent() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val density = context.resources.displayMetrics.density
      val widths = listOf(EmptyListView(context), SnapshotEmptyView(context)).map { view ->
        view.measure(
          View.MeasureSpec.makeMeasureSpec((320 * density).toInt(), View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val content = view.getChildAt(0) as LinearLayout
        val illustration = content.getChildAt(0)
        assertEquals(content.measuredHeight, view.measuredHeight)
        assertTrue(view.measuredHeight < 300 * density)
        val bitmap = Bitmap.createBitmap(illustration.width, illustration.height, Bitmap.Config.ARGB_8888)
        illustration.draw(Canvas(bitmap))
        var left = bitmap.width
        var right = -1
        for (y in 0 until bitmap.height) {
          for (x in 0 until bitmap.width) {
            if ((bitmap.getPixel(x, y) ushr 24) > 20) {
              left = minOf(left, x)
              right = maxOf(right, x)
            }
          }
        }
        bitmap.recycle()
        assertTrue(right > left)
        right - left + 1
      }
      assertEquals(widths[0].toFloat(), widths[1].toFloat(), 2 * density)
    }
  }
}
