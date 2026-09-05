package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.res.Configuration
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItemDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceItemViewInstrumentedTest {

  @Test
  fun longAndShortIdentifiersKeepEqualRowHeightsAndFullText() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      for (fontScale in listOf(1f, 1.5f, 2f)) {
        val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
          this.fontScale = fontScale
        }
        val context = ContextThemeWrapper(
          instrumentation.targetContext.createConfigurationContext(configuration),
          R.style.AppTheme
        )
        val row = LibReferenceItemView(context)
        for (name in listOf(
          "androidx.room.MultiInstanceInvalidationService",
          "androidx.work.impl.background.systemjob.SystemJobService",
          "libc.so"
        )) {
          for (widthDp in listOf(280, 320, 360, 412)) {
            row.bind(display(name), "Service")
            row.measure(
              View.MeasureSpec.makeMeasureSpec(
                (widthDp * context.resources.displayMetrics.density).toInt(),
                View.MeasureSpec.EXACTLY
              ),
              View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            row.layout(0, 0, row.measuredWidth, row.measuredHeight)
            val column = row.children.filterIsInstance<ViewGroup>().single()
            val text = column.children.filterIsInstance<TextView>().last()
            assertEquals(name, text.text.toString())
            val layout = text.layout
            assertEquals(1, layout.lineCount)
            val longRowHeight = row.measuredHeight
            if (name.length > 40 && widthDp == 280) {
              assertTrue(layout.getEllipsisCount(0) > 0)
              assertTrue(layout.getEllipsisStart(0) > 0)
              assertTrue(layout.getEllipsisStart(0) + layout.getEllipsisCount(0) < name.length)
            }
            if (name.contains("Service")) {
              val spanned = text.text as Spanned
              val highlight = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java).single()
              assertEquals("Service", name.substring(spanned.getSpanStart(highlight), spanned.getSpanEnd(highlight)))
            }
            row.bind(display("libc.so"), "")
            assertEquals("libc.so", text.text.toString())
            row.measure(
              View.MeasureSpec.makeMeasureSpec(row.measuredWidth, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            assertEquals("$name at $widthDp dp / $fontScale", longRowHeight, row.measuredHeight)
          }
        }
      }
    }
  }

  private fun display(name: String) = LibReferenceItemDisplay(
    label = if (name.length > 40) "Jetpack library with a very long display label" else "Jetpack",
    italicLabel = false,
    libName = name,
    count = "123",
    iconRes = R.drawable.ic_question,
    iconContentDescription = "Jetpack",
    desaturateIcon = false,
    canOpenDetail = true,
    contentDescription = name
  )
}
