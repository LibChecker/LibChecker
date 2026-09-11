package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDetailTitleViewInstrumentedTest {
  @Test
  fun rendersCountsExpansionAndClearsRecycledHeader() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val section = SnapshotDetailSection(
      type = SERVICE,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services, Added 2, Moved 3, Expanded",
      collapsedDescription = "Services, Added 2, Moved 3, Collapsed",
      items = emptyList(),
      statusCounts = listOf(
        SnapshotDetailStatusCount(
          ADDED,
          2,
          "2",
          "Added",
          SnapshotDetailItemStatusDisplayData(R.drawable.ic_add, R.color.snapshot_status_added, R.string.snapshot_indicator_added)
        ),
        SnapshotDetailStatusCount(
          MOVED,
          3,
          "3",
          "Moved",
          SnapshotDetailItemStatusDisplayData(R.drawable.ic_move, R.color.snapshot_status_moved, R.string.snapshot_indicator_moved)
        )
      )
    )
    lateinit var view: SnapshotDetailTitleView
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      view = SnapshotDetailTitleView(context)
      view.render(section, expanded = false)
      view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
      view.layout(0, 0, view.measuredWidth, view.measuredHeight)
      val arrow = view.getChildAt(2)
      assertEquals(view.paddingLeft.toFloat(), arrow.x, 0.01f)
      assertVisibleStartEdge(arrow)
      val title = view.getChildAt(0) as TextView
      val counts = view.getChildAt(1) as TextView
      assertEquals("Services", title.text.toString())
      assertEquals(View.VISIBLE, counts.visibility)
      assertEquals("+2\uFFFC\uFFFC3", counts.text.toString())
      val text = counts.text as Spanned
      assertEquals(2, text.getSpans(0, text.length, ReplacementSpan::class.java).size)
      assertEquals(
        listOf(context.getColor(R.color.snapshot_status_added), context.getColor(R.color.snapshot_status_moved)),
        text.getSpans(0, text.length, ForegroundColorSpan::class.java).map { it.foregroundColor }
      )
      assertEquals(section.collapsedDescription, view.contentDescription)
      assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, title.importantForAccessibility)
      assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, counts.importantForAccessibility)
      assertEquals(0f, view.getChildAt(2).rotation, 0f)
      view.render(section, expanded = true)
      assertEquals(section.expandedDescription, view.contentDescription)
    }
    val deadline = SystemClock.uptimeMillis() + 2_000
    var expansionFinished = false
    while (!expansionFinished && SystemClock.uptimeMillis() < deadline) {
      instrumentation.runOnMainSync { expansionFinished = view.getChildAt(2).rotation == 90f }
      if (!expansionFinished) SystemClock.sleep(16)
    }
    instrumentation.runOnMainSync {
      assertEquals(90f, view.getChildAt(2).rotation, 0f)
      val arrow = view.getChildAt(2)
      assertEquals(view.paddingLeft.toFloat(), arrow.x, 0.01f)
      assertVisibleStartEdge(arrow)
      view.render(section.copy(title = "Empty", statusCounts = emptyList()), expanded = true)
      assertEquals("Empty", (view.getChildAt(0) as TextView).text.toString())
      assertEquals(View.GONE, view.getChildAt(1).visibility)
      assertEquals("", (view.getChildAt(1) as TextView).text.toString())
      assertEquals(90f, view.getChildAt(2).rotation, 0f)
    }
  }
  private fun assertVisibleStartEdge(arrow: View) {
    val bitmap = Bitmap.createBitmap(arrow.width, arrow.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.rotate(arrow.rotation, arrow.width / 2f, arrow.height / 2f)
    arrow.draw(canvas)
    val firstVisibleColumn = (0 until bitmap.width).first { x ->
      (0 until bitmap.height).any { y -> Color.alpha(bitmap.getPixel(x, y)) >= 128 }
    }
    assertEquals(0, firstVisibleColumn)
    bitmap.recycle()
  }
}
