package com.absinthe.libchecker.ui.preference.view

import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.ui.preference.model.PreferenceItemGroupPosition
import com.absinthe.libchecker.ui.preference.model.PreferenceItemRenderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferenceInlineControlInstrumentedTest {
  @Test
  fun collapsedRowsCreateNoControlsAndReuseChildrenAfterReopening() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme).apply {
        theme.applyStyle(R.style.ThemeOverlay, true)
      }
      val item = LayoutInflater.from(context)
        .inflate(R.layout.preference_m3e, FrameLayout(context), false) as PreferenceItemView
      val inline = item.findViewById<PreferenceInlineControlView>(R.id.settings_preference_inline_control)
      val state = PreferenceItemRenderState(
        preferenceKey = "range",
        title = "Range",
        summary = null,
        toggleChecked = null,
        showChevron = false,
        badgeDescription = null,
        groupPosition = PreferenceItemGroupPosition.SINGLE,
        inlineControl = PreferenceInlineControl.Range(2, 1, 10)
      )
      repeat(10) { item.bind(state) }
      assertEquals(0, inline.childCount)
      item.bind(state.copy(expanded = true))
      assertEquals(1, inline.childCount)
      val child = inline.getChildAt(0)
      item.bind(state)
      item.bind(state.copy(expanded = true))
      assertSame(child, inline.getChildAt(0))
      item.bind(state.copy(inlineControl = PreferenceInlineControl.Range(5, 1, 10), expanded = true))
      assertNotSame(child, inline.getChildAt(0))
    }
  }

  @Test
  fun reusedChoiceDispatchesToLatestCallbackAndReconcilesRejectedSelection() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme).apply {
        theme.applyStyle(R.style.ThemeOverlay, true)
      }
      val inline = PreferenceInlineControlView(context)
      val control = PreferenceInlineControl.DraggableChoice(
        entries = listOf("One", "Two"),
        entryValues = listOf("1", "2"),
        selectedValue = "1"
      )
      var oldCalls = 0
      var selected: String? = null
      inline.bind("Choice", control, { oldCalls++ }, {})
      val child = inline.getChildAt(0)
      inline.bind("Choice", control, { selected = it }, {})
      assertSame(child, inline.getChildAt(0))
      val buttons = (child as FrameLayout).getChildAt(1) as LinearLayout
      buttons.getChildAt(1).performClick()
      assertEquals(0, oldCalls)
      assertEquals("2", selected)
      // The owner can reject a choice. Rebinding its unchanged model must restore that value.
      inline.bind("Choice", control, {}, {})
      assertNotSame(child, inline.getChildAt(0))
    }
  }
}
