package com.absinthe.libchecker.domain.home

import android.content.res.Configuration
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.view.app.FloatingBottomNavigationView
import com.absinthe.libchecker.view.app.FloatingNavigationRailView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainLayoutResourceInstrumentedTest {
  @Test
  fun bindsNavigationAndSelectsIconsAcrossPhoneAndTabletConfigurations() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      for (smallestWidth in listOf(360, 600)) {
        for (landscape in listOf(false, true)) {
          val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
            smallestScreenWidthDp = smallestWidth
            screenWidthDp = if (landscape) 960 else smallestWidth
            screenHeightDp = if (landscape) smallestWidth else 960
            orientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
          }
          val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(configuration), R.style.AppTheme)
          val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
          val rail = landscape || smallestWidth >= 600
          assertEquals(if (rail) FloatingNavigationRailView::class.java else FloatingBottomNavigationView::class.java, binding.navView.javaClass)
          for ((resource, horizontal) in listOf(
            R.drawable.ic_theme_system to R.drawable.ic_theme_system_horizontal,
            R.drawable.ic_floating_bar to R.drawable.ic_floating_bar_horizontal
          )) {
            val selected = TypedValue().also { context.resources.getValue(resource, it, true) }
            val shared = TypedValue().also { context.resources.getValue(horizontal, it, true) }
            assertEquals(rail, selected.string.toString() == shared.string.toString())
          }
        }
      }
    }
  }
}
