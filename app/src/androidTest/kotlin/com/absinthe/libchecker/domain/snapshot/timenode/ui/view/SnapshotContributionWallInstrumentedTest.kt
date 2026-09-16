package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.utils.extensions.dp
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotContributionWallInstrumentedTest {
  @Test
  fun everyDisplayedDayHasAnAccessibleActionAndKeyboardActivation() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val start = LocalDate.of(2026, 9, 7)
      val days = (0L..8L).associate { offset ->
        val date = start.plusDays(offset)
        date to DayContribution(date, updateCount = offset.toInt(), isSnapshotDay = offset == 0L)
      }
      val wall = SnapshotContributionWallView(context)
      var selected: DayContribution? = null
      var anchor: Rect? = null
      wall.setOnDayClickListener { day, bounds ->
        selected = day
        anchor = bounds
      }
      wall.bind(SnapshotContributionData(days, start, start.plusDays(8), emptyMap(), 36))
      wall.measure(
        View.MeasureSpec.makeMeasureSpec(360.dp, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
      )
      wall.layout(0, 0, wall.measuredWidth, wall.measuredHeight)
      val grid = wall.getChildAt(1)
      val provider = grid.accessibilityNodeProvider
      assertNotNull("Dates must be exposed as virtual accessibility nodes", provider)
      assertEquals(9, checkNotNull(provider.createAccessibilityNodeInfo(View.NO_ID)).childCount)
      for (id in days.keys.indices) {
        val node = checkNotNull(provider.createAccessibilityNodeInfo(id))
        assertTrue(node.isClickable)
        assertTrue(node.contentDescription.contains(start.plusDays(id.toLong()).toString()))
        val bounds = Rect()
        node.getBoundsInParent(bounds)
        assertTrue(!bounds.isEmpty && Rect(0, 0, grid.width, grid.height).contains(bounds))
        assertTrue(provider.performAction(id, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(days[start.plusDays(id.toLong())], selected)
        assertTrue(anchor?.isEmpty == false)
      }
      assertTrue(grid.requestFocus())
      assertTrue(provider.performAction(1, AccessibilityNodeInfo.ACTION_FOCUS, null))
      selected = null
      assertTrue(grid.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)))
      assertEquals(days[start.plusDays(1)], selected)
    }
  }
}
