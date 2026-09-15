package com.absinthe.libchecker.view.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.google.android.material.R as MaterialR
import com.google.android.material.navigation.NavigationBarView
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingNavigationColorInstrumentedTest {
  @Test
  fun indicatorColorIsContinuousAtAttachedEndpointWithAndWithoutBlur() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      for (nav in listOf<NavigationBarView>(FloatingBottomNavigationView(context), FloatingNavigationRailView(context))) {
        nav.inflateMenu(R.menu.bottom_nav_menu)
        val floating = nav as FloatingNavigationBar
        val thumb = nav.getChildAt(0).background
        val indicator = nav.findViewById<View>(nav.selectedItemId)
          .findViewById<View>(MaterialR.id.navigation_bar_item_active_indicator_view)
        val nativeColor = indicator.background.centerPixel()
        for (blur in listOf(0f, 0.5f, 1f)) {
          floating.setBlurProgress(blur)
          floating.setFloatingProgress(1f)
          assertEquals((255f - 55f * blur).roundToInt(), Color.alpha(thumb.centerPixel()))
          floating.setFloatingProgress(0.001f)
          // BlurCoordinatorLayout reapplies blur after each navigation animation update.
          floating.setBlurProgress(blur)
          val before = thumb.centerPixel()
          floating.setFloatingProgress(0f)
          val after = indicator.background.centerPixel()
          instrumentation.sendStatus(
            0,
            Bundle().apply {
              putString("indicatorHandoff", "${nav.javaClass.simpleName} blur=$blur before=${Integer.toHexString(before)} after=${Integer.toHexString(after)}")
            }
          )
          assertEquals(nativeColor, after)
          assertEquals("Indicator color jumped with blur=$blur", after, before)
        }
      }
    }
  }

  private fun Drawable.centerPixel(): Int {
    val previousBounds = Rect(bounds)
    val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    return try {
      setBounds(0, 0, 32, 32)
      draw(Canvas(bitmap))
      bitmap.getPixel(16, 16)
    } finally {
      bounds = previousBounds
      bitmap.recycle()
    }
  }
}
