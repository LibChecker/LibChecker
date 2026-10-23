package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.graphics.Color
import android.graphics.Outline
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotTypeIndicatorInstrumentedTest {
  @Test
  fun thinColorLabelUsesHalfHeightForItsRoundedOutline() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val indicator = SnapshotTypeIndicatorView(context)
      indicator.setIndicatorInfo("Added", null, Color.GREEN)
      indicator.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
      )
      indicator.layout(0, 0, indicator.measuredWidth, indicator.measuredHeight)
      val label = indicator.getChildAt(2)
      val outline = Outline()
      label.outlineProvider.getOutline(label, outline)
      assertEquals(label.height / 2f, outline.radius, 0.01f)
    }
  }
}
