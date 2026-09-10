package com.absinthe.libchecker.domain.home

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.view.app.FloatingBottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeImeNavigationInstrumentedTest {
  @Test
  fun listsFollowImeWhileTabsSwitchAndMiniNavigationRestores() {
    val originalFloating = GlobalValues.isFloatingNavBar
    GlobalValues.isFloatingNavBar = true
    try {
      ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        fun awaitState(message: String, condition: (MainActivity) -> Boolean) {
          val deadline = SystemClock.uptimeMillis() + 30_000
          while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            scenario.onActivity { ready = condition(it) }
            if (ready) return
            SystemClock.sleep(50)
          }
          throw AssertionError(message)
        }
        scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        awaitState("Portrait layout unavailable") {
          it.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
            it.findViewById<View>(R.id.nav_view) is FloatingBottomNavigationView
        }
        scenario.onActivity { it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_app_list }
        fun findList(view: View): RecyclerView? {
          if (view is RecyclerView && view.id == android.R.id.list && view.getGlobalVisibleRect(android.graphics.Rect())) return view
          if (view is ViewGroup) for (index in 0 until view.childCount) findList(view.getChildAt(index))?.let { return it }
          return null
        }
        for ((source, target) in listOf(R.id.navigation_app_list to R.id.navigation_snapshot, R.id.navigation_snapshot to R.id.navigation_app_list)) {
          awaitState("Search unavailable") {
            it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId == source &&
              it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search)?.isVisible == true &&
              findList(it.window.decorView) != null
          }
          SystemClock.sleep(600)
          val paddings = mutableListOf<Int>()
          val listener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ -> paddings.add(view.paddingBottom) }
          var normalWidth = 0
          var normalHeight = 0
          lateinit var list: RecyclerView
          scenario.onActivity {
            val nav = it.findViewById<FloatingBottomNavigationView>(R.id.nav_view)
            normalWidth = nav.width
            normalHeight = nav.height
            list = checkNotNull(findList(it.window.decorView))
            list.addOnLayoutChangeListener(listener)
            it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search).expandActionView()
          }
          awaitState("Mini navigation did not appear") { it.findViewById<FloatingBottomNavigationView>(R.id.nav_view).miniProgress >= 0.99f }
          SystemClock.sleep(400)
          var overlappingAnimations = false
          lateinit var animationObserver: android.view.ViewTreeObserver.OnPreDrawListener
          scenario.onActivity {
            val nav = it.findViewById<FloatingBottomNavigationView>(R.id.nav_view)
            val pager = it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewpager)
            animationObserver = android.view.ViewTreeObserver.OnPreDrawListener {
              if (nav.miniProgress > 0f && nav.miniProgress < 1f && pager.alpha < 1f) overlappingAnimations = true
              true
            }
            pager.viewTreeObserver.addOnPreDrawListener(animationObserver)
            val density = it.resources.displayMetrics.density
            assertTrue("Mini navigation is materially narrower", nav.width < normalWidth * 0.8)
            assertTrue("Mini navigation is shorter", nav.height < normalHeight)
            assertTrue("Keep a 48dp touch target", nav.height >= 48 * density - 1)
            assertEquals(4, nav.menu.size())
            val position = IntArray(2)
            nav.getLocationOnScreen(position)
            val root = it.window.decorView
            val imeBottom = ViewCompat.getRootWindowInsets(root)!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
            assertTrue("Navigation remains above IME", position[1] + nav.height <= root.height - imeBottom + 2)
            assertTrue("List padding follows intermediate IME frames: $paddings", paddings.distinct().size > 2)
            paddings.clear()
            nav.findViewById<View>(R.id.navigation_settings).performClick()
            nav.findViewById<View>(target).performClick()
            assertEquals("Tab selection must respond immediately", target, nav.selectedItemId)
          }
          awaitState("Keyboard/tab transition did not finish") {
            val nav = it.findViewById<FloatingBottomNavigationView>(R.id.nav_view)
            nav.selectedItemId == target && nav.miniProgress == 0f &&
              it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewpager).currentItem == (if (target == R.id.navigation_snapshot) 2 else 0) &&
              ViewCompat.getRootWindowInsets(it.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false
          }
          SystemClock.sleep(500)
          scenario.onActivity {
            val nav = it.findViewById<FloatingBottomNavigationView>(R.id.nav_view)
            val pager = it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewpager)
            pager.viewTreeObserver.removeOnPreDrawListener(animationObserver)
            assertTrue("Page and keyboard transitions must overlap", overlappingAnimations)
            assertEquals(normalWidth, nav.width)
            assertEquals(normalHeight, nav.height)
            assertEquals(0f, nav.translationY, 0.01f)
            assertTrue("Closing IME also updates intermediate padding", paddings.distinct().size > 2)
            list.removeOnLayoutChangeListener(listener)
          }
        }
        scenario.onActivity { it.setFloatingNavBarEnabled(false) }
        SystemClock.sleep(500)
        scenario.onActivity { it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search).expandActionView() }
        awaitState("Keyboard did not open with floating navigation disabled") {
          ViewCompat.getRootWindowInsets(it.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        SystemClock.sleep(400)
        scenario.onActivity {
          assertEquals(0f, it.findViewById<FloatingBottomNavigationView>(R.id.nav_view).miniProgress)
          WindowCompat.getInsetsController(it.window, it.window.decorView).hide(WindowInsetsCompat.Type.ime())
        }
        awaitState("Keyboard did not close") {
          ViewCompat.getRootWindowInsets(it.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        awaitState("Landscape navigation unavailable") {
          it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            it.findViewById<View>(R.id.nav_view) !is FloatingBottomNavigationView &&
            it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search)?.isVisible == true
        }
        scenario.onActivity { it.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.search).expandActionView() }
        awaitState("Landscape keyboard did not open") {
          ViewCompat.getRootWindowInsets(it.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        SystemClock.sleep(400)
        scenario.onActivity {
          assertEquals(0f, it.keyboardNavigationOffset, 0.01f)
          WindowCompat.getInsetsController(it.window, it.window.decorView).hide(WindowInsetsCompat.Type.ime())
          it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
      }
    } finally {
      GlobalValues.isFloatingNavBar = originalFloating
    }
  }
}
