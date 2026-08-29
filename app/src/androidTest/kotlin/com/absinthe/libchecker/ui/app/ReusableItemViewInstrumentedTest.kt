package com.absinthe.libchecker.ui.app

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isGone
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AppPropItemView
import com.absinthe.libchecker.domain.app.detail.ui.view.SignatureDetailItemView
import com.absinthe.libchecker.domain.snapshot.track.ui.view.TrackItemView
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReusableItemViewInstrumentedTest {

  @Test
  fun signatureRowAppliesContentSpacingDuringConstruction() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val result = runCatching { SignatureDetailItemView(context) }

      assertNull(result.exceptionOrNull())
      val content = result.getOrThrow().findTextViews()[1]
      val expectedMargin = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP,
        4f,
        context.resources.displayMetrics
      ).toInt()
      assertEquals(expectedMargin, (content.layoutParams as LinearLayout.LayoutParams).topMargin)
    }
  }

  @Test
  fun appPropRowDoesNotReserveHeightForEmptyTextLines() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val row = AppPropItemView(context).apply {
        bind(
          AppPropItem(
            key = "android:name",
            originalDisplayValue = "Example",
            resource = null
          )
        ) {}
      }

      row.measureForTest()
      val heightWithEmptyLines = row.measuredHeight
      row.findEmptyTextViews().forEach { it.isGone = true }
      row.measureForTest()

      assertEquals(row.measuredHeight, heightWithEmptyLines)
    }
  }

  @Test
  fun trackRowUsesBorderlessRestingBackground() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val baseContext = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val context = ContextThemeWrapper(baseContext, R.style.AppListMaterialCard)
      val row = TrackItemView(context)
      val expected = AppCompatResources.getDrawable(
        context,
        context.getResourceIdByAttr(android.R.attr.selectableItemBackground)
      )

      assertEquals(expected?.javaClass, row.background?.javaClass)
    }
  }

  private fun View.measureForTest() {
    measure(
      View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
  }

  private fun View.findEmptyTextViews(): List<TextView> {
    val result = mutableListOf<TextView>()
    if (this is TextView && text.isNullOrEmpty()) {
      result += this
    }
    if (this is ViewGroup) {
      for (index in 0 until childCount) {
        result += getChildAt(index).findEmptyTextViews()
      }
    }
    return result
  }

  private fun View.findTextViews(): List<TextView> {
    val result = mutableListOf<TextView>()
    if (this is TextView) {
      result += this
    }
    if (this is ViewGroup) {
      for (index in 0 until childCount) {
        result += getChildAt(index).findTextViews()
      }
    }
    return result
  }
}
