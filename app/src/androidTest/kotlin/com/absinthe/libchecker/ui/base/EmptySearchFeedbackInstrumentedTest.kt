package com.absinthe.libchecker.ui.base

import android.os.SystemClock
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ActivityScenario
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptySearchFeedbackInstrumentedTest {
  @Test
  fun emptySearchShakesOnlyTheIconAndClearingRestoresItOnAllHomeLists() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      for (destination in listOf(R.id.navigation_app_list, R.id.navigation_classify, R.id.navigation_snapshot)) {
        scenario.onActivity {
          it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = destination
        }
        SystemClock.sleep(600)
        var ready = false
        val deadline = SystemClock.uptimeMillis() + 30_000
        while (!ready && SystemClock.uptimeMillis() < deadline) {
          scenario.onActivity {
            ready = it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search)?.isVisible == true
          }
          SystemClock.sleep(50)
        }
        assertTrue("Search must be ready on destination $destination", ready)
        lateinit var search: SearchView
        lateinit var icon: ImageView
        scenario.onActivity {
          val item = it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search)
          item.expandActionView()
          search = item.actionView as SearchView
          search.setQuery("", false)
          search.requestFocus()
          icon = search.findViewById(androidx.appcompat.R.id.search_mag_icon)
        }
        SystemClock.sleep(500)
        var minRotation = 0f
        var maxRotation = 0f
        val handleTip = FloatArray(2)
        val frameObserver = ViewTreeObserver.OnPreDrawListener {
          minRotation = minOf(minRotation, icon.rotation)
          maxRotation = maxOf(maxRotation, icon.rotation)
          assertEquals("The icon must not move horizontally", 0f, icon.translationX, 0f)
          assertTrue("Rotation must stay within 10 degrees", icon.rotation in -10f..10f)
          val transformedTip = handleTip.copyOf()
          icon.matrix.mapPoints(transformedTip)
          assertEquals("Handle must stay anchored horizontally", handleTip[0], transformedTip[0], 0.01f)
          assertEquals("Handle must stay anchored vertically", handleTip[1], transformedTip[1], 0.01f)
          assertEquals("The text field must stay still", 0f, search.translationX, 0f)
          true
        }
        scenario.onActivity {
          assertTrue("Expanded search icon must be visible", icon.isShown)
          val bounds = icon.drawable.bounds
          handleTip[0] = bounds.left + bounds.width() * 19.75f / 24f
          handleTip[1] = bounds.top + bounds.height() * 19.75f / 24f
          icon.imageMatrix.mapPoints(handleTip)
          handleTip[0] += icon.paddingLeft
          handleTip[1] += icon.paddingTop
          icon.viewTreeObserver.addOnPreDrawListener(frameObserver)
          search.setQuery("lc_no_matching_result_7d529", false)
        }
        SystemClock.sleep(3_000)
        scenario.onActivity {
          icon.viewTreeObserver.removeOnPreDrawListener(frameObserver)
          assertTrue("Icon must rotate left on destination $destination: $minRotation", minRotation < -4f)
          assertTrue("Icon must rotate right on destination $destination: $maxRotation", maxRotation > 4f)
          assertEquals(0f, icon.rotation, 0f)
          search.setQuery("", false)
        }
        val clearDeadline = SystemClock.uptimeMillis() + 800
        while (SystemClock.uptimeMillis() < clearDeadline) {
          scenario.onActivity { assertEquals("Clearing must not shake", 0f, icon.rotation, 0f) }
          SystemClock.sleep(16)
        }
        scenario.onActivity {
          it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search).collapseActionView()
        }
      }
    }
  }
}
