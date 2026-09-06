package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.utils.extensions.getDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDetailItemViewInstrumentedTest {
  @Test
  fun rendersMovedPathAndChipStylesThenClearsRecycledState() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val view = SnapshotDetailItemView(context)
      val item = SnapshotDetailItem("new.Service", "old.Service → new.Service", "Extra", MOVED, SERVICE, "old.Service")
      val display = SnapshotDetailItemDisplayData(
        item,
        item.title,
        item.extra,
        "Full accessible description",
        "Original report",
        SnapshotDetailItemStatusDisplayData(R.drawable.ic_move, R.color.snapshot_status_moved, R.string.snapshot_indicator_moved),
        null
      )
      var clicks = 0
      view.setChipOnClickListener { clicks++ }
      // Simple icons must remain tinted even when colorful icons are enabled.
      listOf(true to false, true to true, false to true, false to false).forEach { (simple, colorful) ->
        view.render(display.copy(ruleChip = SnapshotDetailRuleChipDisplayData("Rule", R.drawable.ic_add, "regex", simple, colorful)))
        assertEquals(display.description, view.contentDescription)
        val textViews = view.children.filterIsInstance<TextView>().toList()
        assertTrue(textViews.any { it.isVisible && it.text.toString().replace("\u200B", "") == "new.Service" })
        assertTrue(textViews.any { it.isVisible && it.text.toString().replace("\u200B", "") == "old" })
        val chip = textViews.single { it.text.toString() == "Rule" }
        val actual = chip.compoundDrawablesRelative[0]
        val expected = requireNotNull(R.drawable.ic_add.getDrawable(context)).mutate().apply {
          clearColorFilter()
          DrawableCompat.setTintList(this, null)
          if (simple) {
            DrawableCompat.setTint(this, chip.currentTextColor)
          } else if (!colorful) {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
          }
        }
        assertTrue(expected.toBitmap(actual.bounds.width(), actual.bounds.height()).sameAs(actual.toBitmap(actual.bounds.width(), actual.bounds.height())))
        chip.performClick()
      }
      assertEquals(4, clicks)
      view.render(display.copy(item = item.copy(diffType = ADDED), title = "Ordinary", extra = "", ruleChip = null))
      assertFalse(view.children.filterIsInstance<TextView>().any { it.isVisible && it.text.toString() in listOf("old", "Rule", "Extra") })
      assertTrue(view.children.filterIsInstance<TextView>().any { it.text.toString() == "Ordinary" })
    }
  }
}
