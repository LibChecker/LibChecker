package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.res.Configuration
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.descendants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetState
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeHeaderState
import com.absinthe.libchecker.utils.extensions.dp
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotContributionWallInstrumentedTest {
  @Test
  fun firstBindReservesTheLoadedWallHeightOnlyInSnapshotMode() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val target = instrumentation.targetContext
      val configuration = Configuration(target.resources.configuration).apply {
        fontScale = 2f
        setLocale(Locale.ENGLISH)
      }
      val context = ContextThemeWrapper(target.createConfigurationContext(configuration), R.style.AppTheme)
      val sheet = TimeNodeBottomSheetView(context)
      val wall = sheet.getChildAt(1) as SnapshotContributionWallView
      val grid = wall.getChildAt(1)
      val state = TimeNodeBottomSheetState("Snapshots", TimeNodeHeaderState.AutoRemove(-1))
      val list = sheet.getChildAt(2)
      val hint = context.getString(R.string.snapshot_contribution_hint)
      fun listTop(): Int {
        sheet.measure(
          View.MeasureSpec.makeMeasureSpec(280.dp, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(660.dp, View.MeasureSpec.EXACTLY)
        )
        sheet.layout(0, 0, sheet.measuredWidth, sheet.measuredHeight)
        return list.top
      }
      sheet.bind(state) {}
      assertEquals(View.VISIBLE, wall.visibility)
      val loadingListTop = listTop()
      assertFalse(grid.isClickable)
      assertFalse(grid.isFocusable)
      assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, grid.importantForAccessibility)
      assertFalse(wall.descendants.filterIsInstance<TextView>().any { it.visibility == View.VISIBLE && it.text == hint })
      assertEquals(0, grid.accessibilityNodeProvider.createAccessibilityNodeInfo(View.NO_ID)?.childCount)
      val start = LocalDate.of(2026, 1, 5)
      for (count in 120..126) {
        val end = start.plusDays(count - 1L)
        val day = DayContribution(end, updateCount = 12345, isSnapshotDay = true)
        val contribution = SnapshotContributionData(mapOf(end to day), start, end, emptyMap(), day.updateCount)
        sheet.bind(state.copy(contributionData = contribution, selectedDate = end)) {}
        assertEquals(loadingListTop, listTop())
        assertTrue(grid.isClickable && grid.isFocusable)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, grid.importantForAccessibility)
      }
      sheet.bind(state) {}
      assertFalse(grid.isFocusable)
      assertEquals(loadingListTop, listTop())
      sheet.bind(state.copy(header = TimeNodeHeaderState.AddApk(true))) {}
      assertEquals(View.GONE, wall.visibility)
    }
  }

  @Test
  fun everyDisplayedDayHasAnAccessibleActionAndKeyboardActivation() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val start = LocalDate.ofEpochDay(20703L)
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
        val location = IntArray(2).also(grid::getLocationOnScreen)
        val screenBounds = Rect().also(node::getBoundsInScreen)
        bounds.offset(location[0] - grid.scrollX, location[1] - grid.scrollY)
        assertEquals(bounds, screenBounds)
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
