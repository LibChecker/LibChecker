package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.app.list.ui.AdvancedMenuBSDFragment
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceMenuBottomSheetState
import com.absinthe.libchecker.domain.statistics.reference.ui.LibReferenceFragment
import com.absinthe.libchecker.domain.statistics.reference.ui.LibReferenceMenuBSDFragment
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.ui.base.ThemeTransitionController
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceTreemapInstrumentedTest {
  @Test
  fun mouseWheelZoomsInStagesAndReturnsToDefault() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val view = LibReferenceTreemapView(activity)
        activity.setContentView(view)
        view.layout(0, 0, 600, 900)
        view.submitReferences((1..80).map { LibReference("lib$it", null, (1..(it + 5)).map { n -> "pkg$n" }.toSet(), NATIVE) })
        fun field(name: String) = LibReferenceTreemapView::class.java.getDeclaredField(name).apply { isAccessible = true }
        val history = field("zoomHistory").get(view) as List<*>
        fun scroll(amount: Float) {
          val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
          }
          val coords = MotionEvent.PointerCoords().apply {
            x = 450f
            y = 650f
            setAxisValue(MotionEvent.AXIS_VSCROLL, amount)
          }
          val now = SystemClock.uptimeMillis()
          val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1, arrayOf(properties), arrayOf(coords), 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0)
          try {
            assertTrue(view.onGenericMotionEvent(event))
          } finally {
            event.recycle()
          }
        }
        scroll(-1f)
        assertTrue(history.isEmpty())
        scroll(1f)
        val animator = field("zoomAnimator").get(view) as ValueAnimator
        assertTrue("Wheel zoom must animate", animator.isRunning)
        scroll(1f)
        assertSame("Rapid wheel events must not restart the transition", animator, field("zoomAnimator").get(view))
        animator.end()
        assertEquals(1, history.size)
        val selected = field("visibleIds").get(view) as Set<*>
        assertTrue(selected.isNotEmpty() && selected.size < 80)
        scroll(-1f)
        (field("zoomAnimator").get(view) as ValueAnimator).end()
        assertTrue(history.isEmpty())
        assertEquals(null, field("visibleIds").get(view))
        scroll(-1f)
        assertTrue(history.isEmpty())
      }
    }
  }

  @Test
  fun keyboardViewportReflowAfterFilteringMatchesFreshCompactLayout() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val view = LibReferenceTreemapView(activity)
        activity.setContentView(view)
        val references = (1..40).map { LibReference("lib$it", null, (1..(it + 5)).map { n -> "pkg$n" }.toSet(), NATIVE) }
        view.layout(0, 0, 1200, 500)
        view.submitReferences(references)
        view.layout(0, 0, 1200, 70)
        val filtered = references.take(25)
        view.submitReferences(filtered)
        view.layout(0, 0, 1200, 500)
        view.reflowForViewport()
        val animatorField = LibReferenceTreemapView::class.java.getDeclaredField("filterAnimator").apply { isAccessible = true }
        (animatorField.get(view) as ValueAnimator).end()
        val fresh = LibReferenceTreemapView(activity)
        fresh.layout(0, 0, 1200, 500)
        fresh.submitReferences(filtered)
        val bitmap = Bitmap.createBitmap(1200, 500, Bitmap.Config.ARGB_8888)
        val expected = Bitmap.createBitmap(1200, 500, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        fresh.draw(Canvas(expected))
        assertTrue("Closing the keyboard must restore a compact layout, not stretch the thin viewport partition", bitmap.sameAs(expected))
        bitmap.recycle()
        expected.recycle()
      }
    }
  }

  @Test
  fun resizingFadesContentAcrossVisibilityThresholdAndSettlesFully() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val view = LibReferenceTreemapView(activity)
        activity.setContentView(view)
        view.submitReferences(listOf(LibReference("test", null, (1..10).map { "pkg$it" }.toSet(), NATIVE)))
        val animatorField = LibReferenceTreemapView::class.java.getDeclaredField("contentFadeAnimator").apply { isAccessible = true }
        val cellsField = LibReferenceTreemapView::class.java.getDeclaredField("cells").apply { isAccessible = true }
        fun opacity(): Float {
          val cell = (cellsField.get(view) as List<*>).single()!!
          return cell.javaClass.getDeclaredField("contentOpacity").apply { isAccessible = true }.getFloat(cell)
        }
        view.layout(0, 0, 600, 900)
        assertEquals(1f, opacity())
        val initialCells = cellsField.get(view) as List<*>
        val initialCell = initialCells.single()
        for ((height, target) in listOf(20 to 0f, 900 to 1f)) {
          view.layout(0, 0, 600, height)
          val animator = animatorField.get(view) as ValueAnimator
          animator.pause()
          animator.currentPlayTime = 75
          view.layout(0, 0, 600, if (target == 0f) height + 1 else height - 1)
          assertSame("Height changes reuse the same cell list", initialCells, cellsField.get(view))
          assertSame("Height changes reuse existing cell objects", initialCell, (cellsField.get(view) as List<*>).single())
          assertSame("Unchanged visibility target must not restart the fade", animator, animatorField.get(view))
          assertTrue("Threshold crossing must have intermediate opacity", opacity() > 0f && opacity() < 1f)
          animator.end()
          assertEquals(target, opacity())
        }
      }
    }
  }

  @Test
  fun filteringAnimatesCellsWithoutFadingTheWholeViewAndKeepsLatestResult() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val view = LibReferenceTreemapView(activity)
        activity.setContentView(view)
        val references = (1..8).map { LibReference("lib$it.so", null, (1..(it + 5)).map { n -> "pkg$n" }.toSet(), NATIVE) }
        view.submitReferences(references)
        view.layout(0, 0, 600, 900)
        val field = LibReferenceTreemapView::class.java.getDeclaredField("filterAnimator").apply { isAccessible = true }
        view.submitReferences(references.take(4), animate = true)
        val first = field.get(view) as ValueAnimator
        first.pause()
        first.currentPlayTime = 125
        assertEquals(1f, view.alpha)
        val bitmap = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertTrue("Retained tiles remain opaque", (bitmap.getPixel(300, 200) ushr 24) == 255)
        view.submitReferences(references.takeLast(2), animate = true)
        (field.get(view) as ValueAnimator).end()
        assertEquals(null, field.get(view))
        val referencesField = LibReferenceTreemapView::class.java.getDeclaredField("references").apply { isAccessible = true }
        assertEquals(references.takeLast(2), referencesField.get(view))
        bitmap.recycle()
      }
    }
  }

  @Test
  fun pinchHintUsesPortraitBottomAndLandscapeRightAndDismissesOnTouch() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        for (landscape in listOf(false, true)) {
          val configuration = Configuration(activity.resources.configuration).apply {
            orientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
          }
          val view = LibReferenceTreemapView(android.view.ContextThemeWrapper(activity.createConfigurationContext(configuration), com.google.android.material.R.style.Theme_Material3_DayNight))
          activity.setContentView(view)
          view.submitReferences(listOf(LibReference("libtest.so", null, (1..10).map { "pkg$it" }.toSet(), NATIVE)))
          val width = if (landscape) 900 else 600
          val height = if (landscape) 600 else 900
          view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
          view.layout(0, 0, width, height)
          fun snapshot(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
          val before = snapshot()
          assertTrue(view.showPinchHint())
          assertFalse(view.showPinchHint())
          val field = LibReferenceTreemapView::class.java.getDeclaredField("pinchHintAnimator").apply { isAccessible = true }
          (field.get(view) as ValueAnimator).apply {
            assertEquals(ValueAnimator.INFINITE, repeatCount)
            pause()
            currentPlayTime = 800
          }
          val during = snapshot()
          var changedPixels = 0
          for (y in 0 until height) {
            for (x in 0 until width) {
              if (before.getPixel(x, y) != during.getPixel(x, y)) {
                changedPixels++
                assertTrue("Hint must stay in the requested half", if (landscape) x >= width / 2 else y >= height / 2)
              }
            }
          }
          assertTrue("Hint must actually be drawn", changedPixels > 0)
          val now = SystemClock.uptimeMillis()
          val touch = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
          view.onTouchEvent(touch)
          touch.recycle()
          assertEquals(null, field.get(view))
          before.recycle()
          during.recycle()
        }
      }
    }
  }

  @Test
  fun resizingRetainsRegionAndReversedPinchClearsPreview() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val view = LibReferenceTreemapView(activity)
        val references = (1..8).map { LibReference("lib$it.so", null, (1..(it + 5)).map { n -> "pkg$n" }.toSet(), NATIVE) }
        view.submitReferences(references)
        view.layout(0, 0, 600, 900)
        fun field(name: String) = LibReferenceTreemapView::class.java.getDeclaredField(name).apply { isAccessible = true }
        fun invoke(name: String) = LibReferenceTreemapView::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(view)
        val partition = field("partition").get(view)
        val selected = setOf("$NATIVE:lib8.so")
        field("visibleIds").set(view, selected)
        @Suppress("UNCHECKED_CAST")
        val history = field("zoomHistory").get(view) as MutableList<Set<String>?>
        history.add(null)
        LibReferenceTreemapView::class.java.getDeclaredMethod("rebuildCells", Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(view, false)
        view.layout(0, 0, 900, 600)
        view.layout(0, 0, 600, 900)
        view.submitReferences(references.map { it.copy() })
        assertSame(partition, field("partition").get(view))
        assertEquals(selected, field("visibleIds").get(view))
        assertEquals(1, history.size)
        field("zoom").setFloat(view, 0.6f)
        invoke("updateZoomPreview")
        assertNotNull(field("previewCells").get(view))
        field("zoomFocusX").setFloat(view, 300f)
        field("zoomFocusY").setFloat(view, 450f)
        field("zoom").setFloat(view, 1.1f)
        invoke("updateZoomPreview")
        assertEquals(null, field("previewCells").get(view))
        assertEquals(0f, field("previewProgress").getFloat(view))
      }
    }
  }

  @Test
  fun freshLaunchRemembersBothDisplayModes() {
    fun findReference(manager: FragmentManager): LibReferenceFragment? {
      manager.fragments.forEach { fragment ->
        if (fragment is LibReferenceFragment) return fragment
        findReference(fragment.childFragmentManager)?.let { return it }
      }
      return null
    }
    for (treemap in listOf(true, false)) {
      ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.onActivity { activity ->
          activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify
          activity.supportFragmentManager.executePendingTransactions()
        }
        val deadline = SystemClock.uptimeMillis() + 30_000
        var ready = false
        while (!ready && SystemClock.uptimeMillis() < deadline) {
          SystemClock.sleep(100)
          scenario.onActivity { activity ->
            ready = findReference(activity.supportFragmentManager)?.takeIf { it.view != null }
              ?.binding?.vfContainer?.displayedChild?.let { it != 0 } == true
          }
        }
        assertTrue("Reference page did not settle", ready)
        scenario.onActivity { activity ->
          val fragment = checkNotNull(findReference(activity.supportFragmentManager))
          fragment.onMenuItemSelected(PopupMenu(activity, fragment.requireView()).menu.add(0, R.id.filter, 0, ""))
          activity.supportFragmentManager.executePendingTransactions()
          val menu = activity.supportFragmentManager.findFragmentByTag(AdvancedMenuBSDFragment::class.java.name) as LibReferenceMenuBSDFragment
          val button = menu.root.findViewById<View>(R.id.lib_reference_display_mode)
          val action = if (treemap) R.string.lib_reference_switch_to_treemap else R.string.lib_reference_switch_to_list
          if (button.contentDescription == activity.getString(action)) button.performClick()
          menu.dismiss()
        }
      }
      ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.onActivity { it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify }
        val deadline = SystemClock.uptimeMillis() + 30_000
        var ready = false
        while (!ready && SystemClock.uptimeMillis() < deadline) {
          SystemClock.sleep(100)
          scenario.onActivity { activity ->
            findReference(activity.supportFragmentManager)?.takeIf { it.view != null }?.let { fragment ->
              ready = (if (treemap) fragment.binding.treemap else fragment.binding.list).isShown
            }
          }
        }
        assertTrue("Fresh launch did not restore treemap=$treemap", ready)
      }
    }
  }

  @Test
  fun recreationRestoresTreemapMode() {
    fun findReference(manager: FragmentManager): LibReferenceFragment? {
      manager.fragments.forEach { fragment ->
        if (fragment is LibReferenceFragment) return fragment
        findReference(fragment.childFragmentManager)?.let { return it }
      }
      return null
    }
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify }
      fun awaitReady(check: (LibReferenceFragment) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 30_000
        while (SystemClock.uptimeMillis() < deadline) {
          var ready = false
          scenario.onActivity { activity ->
            findReference(activity.supportFragmentManager)?.takeIf { it.view != null }?.let { ready = check(it) }
          }
          if (ready) return
          SystemClock.sleep(100)
        }
        throw AssertionError("Recreated treemap did not settle")
      }
      awaitReady { it.binding.vfContainer.displayedChild != 0 }
      scenario.onActivity { activity ->
        val fragment = checkNotNull(findReference(activity.supportFragmentManager))
        fragment.onMenuItemSelected(PopupMenu(activity, fragment.requireView()).menu.add(0, R.id.filter, 0, ""))
        activity.supportFragmentManager.executePendingTransactions()
        val menu = activity.supportFragmentManager.findFragmentByTag(AdvancedMenuBSDFragment::class.java.name) as LibReferenceMenuBSDFragment
        val button = menu.root.findViewById<View>(R.id.lib_reference_display_mode)
        if (button.contentDescription == activity.getString(R.string.lib_reference_switch_to_treemap)) button.performClick()
        menu.dismiss()
        activity.supportFragmentManager.executePendingTransactions()
      }
      scenario.recreate()
      awaitReady { it.binding.treemap.isShown && it.binding.treemap.height > 0 }
      scenario.onActivity { activity ->
        assertFalse(checkNotNull(findReference(activity.supportFragmentManager)).isAllowRefreshing())
      }
      val originalMode = AppCompatDelegate.getDefaultNightMode()
      try {
        for (mode in listOf(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_NO)) {
          scenario.onActivity { activity ->
            activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_settings
          }
          InstrumentationRegistry.getInstrumentation().waitForIdleSync()
          scenario.onActivity { activity -> ThemeTransitionController.applyNightMode(activity, mode) }
          val deadline = SystemClock.uptimeMillis() + 30_000
          var applied = false
          while (!applied && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
            scenario.onActivity { activity ->
              val expected = if (mode == AppCompatDelegate.MODE_NIGHT_YES) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
              applied = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == expected
            }
          }
          assertTrue("Theme change did not settle", applied)
          InstrumentationRegistry.getInstrumentation().waitForIdleSync()
          scenario.onActivity { activity ->
            activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify
          }
          awaitReady { it.binding.treemap.isShown && it.binding.treemap.height > 0 }
        }
      } finally {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { AppCompatDelegate.setDefaultNightMode(originalMode) }
      }
    }
  }

  @Test
  fun realPageSwitchesSearchesAndKeepsListPosition() {
    val hintWasShown = GlobalValues.libReferencePinchHintShown
    GlobalValues.libReferencePinchHintShown = true
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ) as MainActivity
    fun findReference(manager: FragmentManager): LibReferenceFragment? {
      manager.fragments.forEach { fragment ->
        if (fragment is LibReferenceFragment) return fragment
        findReference(fragment.childFragmentManager)?.let { return it }
      }
      return null
    }
    fun awaitState(condition: () -> Boolean) {
      val deadline = SystemClock.uptimeMillis() + 30_000
      while (SystemClock.uptimeMillis() < deadline) {
        var ready = false
        instrumentation.runOnMainSync { ready = condition() }
        if (ready) return
        SystemClock.sleep(100)
      }
      throw AssertionError("Reference page state did not settle")
    }
    lateinit var fragment: LibReferenceFragment
    fun toggle(toTreemap: Boolean) {
      instrumentation.runOnMainSync {
        fragment.onMenuItemSelected(PopupMenu(activity, fragment.requireView()).menu.add(0, R.id.filter, 0, ""))
        activity.supportFragmentManager.executePendingTransactions()
        val menu = activity.supportFragmentManager.findFragmentByTag(AdvancedMenuBSDFragment::class.java.name) as LibReferenceMenuBSDFragment
        val button = menu.root.findViewById<View>(R.id.lib_reference_display_mode)
        val action = activity.getString(if (toTreemap) R.string.lib_reference_switch_to_treemap else R.string.lib_reference_switch_to_list)
        if (button.contentDescription == action) button.performClick()
        val actual = activity.findViewById<NavigationBarView>(R.id.nav_view).menu.findItem(R.id.navigation_classify).icon!!.toBitmap(24, 24)
        val expected = activity.getDrawable(if (toTreemap) R.drawable.ic_reference_treemap else R.drawable.ic_reference_list)!!.toBitmap(24, 24)
        for (y in 0 until 24) {
          for (x in 0 until 24) {
            assertEquals("Navigation icon follows the reference mode", expected.getPixel(x, y) ushr 24, actual.getPixel(x, y) ushr 24)
          }
        }
        menu.dismiss()
      }
      instrumentation.waitForIdleSync()
    }
    try {
      instrumentation.runOnMainSync {
        activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify
      }
      awaitState {
        val found = findReference(activity.supportFragmentManager)
        if (found == null || found.view == null) {
          false
        } else {
          fragment = found
          fragment.binding.vfContainer.displayedChild != 0
        }
      }
      toggle(false)
      var position = 0
      instrumentation.runOnMainSync {
        val list = fragment.binding.list
        (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(minOf(12, list.adapter!!.itemCount - 1).coerceAtLeast(0), 0)
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        position = (fragment.binding.list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
      }
      toggle(true)
      awaitState { fragment.binding.treemap.isShown && fragment.binding.treemap.height > 0 }
      fun snapshot(): Bitmap {
        lateinit var bitmap: Bitmap
        instrumentation.runOnMainSync {
          val tree = fragment.binding.treemap
          bitmap = Bitmap.createBitmap(tree.width, tree.height, Bitmap.Config.ARGB_8888)
          tree.draw(Canvas(bitmap))
        }
        return bitmap
      }
      instrumentation.waitForIdleSync()
      // Wait for the bottom-sheet and navigation transitions before comparing settled pixels.
      SystemClock.sleep(600)
      val beforeTab = snapshot()
      val transitionSizes = mutableListOf<Pair<Int, Int>>()
      val sizeListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        transitionSizes.add(view.width to view.height)
      }
      instrumentation.runOnMainSync {
        fragment.binding.treemap.addOnLayoutChangeListener(sizeListener)
        activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_settings
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        activity.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify
      }
      instrumentation.waitForIdleSync()
      awaitState { fragment.binding.treemap.isShown }
      SystemClock.sleep(600)
      val afterTab = snapshot()
      instrumentation.runOnMainSync {
        fragment.binding.treemap.removeOnLayoutChangeListener(sizeListener)
        assertTrue("Tab animation must not resize the tree: $transitionSizes", transitionSizes.all { it == beforeTab.width to beforeTab.height })
      }
      assertTrue("Returning to the reference tab must preserve every tile and its content", beforeTab.sameAs(afterTab))
      beforeTab.recycle()
      afterTab.recycle()
      val imeHeights = mutableListOf<Int>()
      val imeListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ -> imeHeights.add(view.height) }
      instrumentation.runOnMainSync {
        fragment.binding.treemap.addOnLayoutChangeListener(imeListener)
        activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).menu.findItem(R.id.search).expandActionView()
      }
      awaitState {
        androidx.core.view.ViewCompat.getRootWindowInsets(fragment.requireView())?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
      }
      SystemClock.sleep(600)
      instrumentation.runOnMainSync {
        assertTrue("Keyboard opening should resize through intermediate heights: $imeHeights", imeHeights.distinct().size > 2)
        imeHeights.clear()
        androidx.core.view.WindowCompat.getInsetsController(activity.window, fragment.requireView()).hide(androidx.core.view.WindowInsetsCompat.Type.ime())
      }
      awaitState {
        androidx.core.view.ViewCompat.getRootWindowInsets(fragment.requireView())?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == false
      }
      SystemClock.sleep(600)
      instrumentation.runOnMainSync {
        fragment.binding.treemap.removeOnLayoutChangeListener(imeListener)
        assertTrue("Keyboard closing should resize through intermediate heights: $imeHeights", imeHeights.distinct().size > 2)
        val tree = fragment.binding.treemap
        val partitionHeight = LibReferenceTreemapView::class.java.getDeclaredField("partitionHeight").apply { isAccessible = true }.getInt(tree)
        assertEquals("IME completion rebuilds against the final viewport", tree.height - tree.paddingTop - tree.paddingBottom, partitionHeight)
        activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).menu.findItem(R.id.search).collapseActionView()
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        assertFalse(fragment.isAllowRefreshing())
        assertEquals(null, fragment.getSuitableLayoutManager())
        val tree = fragment.binding.treemap
        val treeLocation = IntArray(2)
        val chromeLocation = IntArray(2)
        tree.getLocationOnScreen(treeLocation)
        val appbar = activity.findViewById<View>(R.id.appbar)
        appbar.getLocationOnScreen(chromeLocation)
        assertTrue("Tree starts below toolbar", treeLocation[1] >= chromeLocation[1] + appbar.height)
        val nav = activity.findViewById<View>(R.id.nav_view)
        nav.getLocationOnScreen(chromeLocation)
        if (nav.width > nav.height) assertTrue("Tree ends above navigation", treeLocation[1] + tree.height <= chromeLocation[1])
        fragment.onQueryTextChange("libchecker-no-such-reference-917263")
      }
      awaitState { fragment.binding.treemapEmpty.isShown }
      instrumentation.runOnMainSync { fragment.onQueryTextChange("") }
      awaitState { fragment.binding.treemap.isShown && !fragment.binding.treemapEmpty.isShown }
      // Search intentionally resets the data set. Verify position retention on a pure mode switch.
      toggle(false)
      instrumentation.runOnMainSync {
        (fragment.binding.list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position.coerceAtLeast(0), 0)
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        position = (fragment.binding.list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
      }
      toggle(true)
      toggle(false)
      instrumentation.runOnMainSync {
        assertEquals(position, (fragment.binding.list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
        assertTrue(fragment.isAllowRefreshing())
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
      GlobalValues.libReferencePinchHintShown = hintWasShown
    }
  }

  @Test
  fun virtualNodesActivateReferencesAndSwipeDoesNotClickOrScroll() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    lateinit var view: LibReferenceTreemapView
    var selected: LibReference? = null
    val references = listOf(
      LibReference("liblarge.so", null, (1..9).map { "app$it" }.toSet(), NATIVE),
      LibReference("libsmall.so", null, (1..6).map { "app$it" }.toSet(), NATIVE)
    )
    try {
      instrumentation.runOnMainSync {
        view = LibReferenceTreemapView(activity).apply {
          onReferenceClick = { selected = it }
          submitReferences(references)
        }
        activity.addContentView(view, ViewGroup.LayoutParams(600, 900))
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        assertTrue(view.isLaidOut)
        val provider = view.accessibilityNodeProvider
        assertNotNull(provider)
        assertEquals(2, checkNotNull(provider.createAccessibilityNodeInfo(-1)).childCount)
        val largeNode = checkNotNull(provider.createAccessibilityNodeInfo(0))
        assertEquals("liblarge.so, 9", largeNode.contentDescription.toString())
        assertEquals("android.widget.Button", largeNode.className.toString())
        assertEquals("libsmall.so, 6", checkNotNull(provider.createAccessibilityNodeInfo(1)).contentDescription.toString())
        assertTrue(provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertSame(references[0], selected)
        assertTrue(provider.performAction(1, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertSame(references[1], selected)
        selected = null
        val bounds = Rect()
        largeNode.getBoundsInParent(bounds)
        val y = bounds.exactCenterY()
        val x = bounds.left + bounds.width() / 4f
        val endX = bounds.right - bounds.width() / 4f
        val downTime = SystemClock.uptimeMillis()
        fun touch(action: Int, position: Float, elapsed: Long) {
          val event = MotionEvent.obtain(downTime, downTime + elapsed, action, position, y, 0)
          view.dispatchTouchEvent(event)
          event.recycle()
        }
        touch(MotionEvent.ACTION_DOWN, x, 0)
        touch(MotionEvent.ACTION_MOVE, endX, 100)
        touch(MotionEvent.ACTION_UP, endX, 200)
        assertEquals(null, selected)
        touch(MotionEvent.ACTION_DOWN, x, 300)
        touch(MotionEvent.ACTION_UP, x, 350)
        assertSame(references[0], selected)
        assertEquals(0, view.scrollX)
        assertEquals(0, view.scrollY)
        listOf(-1, 1).forEach { direction ->
          assertFalse(view.canScrollVertically(direction))
          assertFalse(view.canScrollHorizontally(direction))
        }
        selected = null
        view.submitReferences(listOf(LibReference("small", null, setOf("a", "b", "c"), NATIVE)))
        val otherNode = checkNotNull(provider.createAccessibilityNodeInfo(0))
        assertEquals(activity.getString(R.string.lib_reference_other_description, 1, 3L), otherNode.contentDescription)
        assertFalse(otherNode.isClickable)
        assertFalse(provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(null, selected)
        view.submitReferences(listOf(references.first()))
        fun interiorColorCount(sizeDp: Int, heightDp: Int = sizeDp): Int {
          val size = (sizeDp * activity.resources.displayMetrics.density).toInt()
          val height = (heightDp * activity.resources.displayMetrics.density).toInt()
          view.layout(0, 0, size, height)
          val bitmap = Bitmap.createBitmap(size, height, Bitmap.Config.ARGB_8888)
          view.draw(Canvas(bitmap))
          val pixels = IntArray(size * height)
          bitmap.getPixels(pixels, 0, size, 0, 0, size, height)
          val inset = (12 * activity.resources.displayMetrics.density).toInt()
          val colors = mutableSetOf<Int>()
          for (y in inset until height - inset) {
            for (x in inset until size - inset) colors += pixels[y * size + x]
          }
          bitmap.recycle()
          return colors.size
        }
        assertEquals("Small cells contain only their background", 1, interiorColorCount(32))
        assertTrue("Wide short cells draw content when it fits", interiorColorCount(100, 48) > 1)
        assertTrue("Large cells draw icon and text", interiorColorCount(160) > 1)
        view.submitReferences(references)
        val beforeZoom = Rect().also { provider.createAccessibilityNodeInfo(0)!!.getBoundsInParent(it) }
        val center = view.width / 2f
        val gestureStart = SystemClock.uptimeMillis()
        fun pinchEvent(action: Int, span: Float, elapsed: Long) {
          val count = if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) 1 else 2
          val properties = Array(count) { index ->
            MotionEvent.PointerProperties().apply {
              id = index
              toolType = MotionEvent.TOOL_TYPE_FINGER
            }
          }
          val coordinates = Array(count) { index ->
            MotionEvent.PointerCoords().apply {
              this.x = center + if (index == 0) -span else span
              this.y = center
              pressure = 1f
              size = 1f
            }
          }
          val event = MotionEvent.obtain(gestureStart, gestureStart + elapsed, action, count, properties, coordinates, 0, 0, 1f, 1f, 0, 0, 0, 0)
          view.onTouchEvent(event)
          event.recycle()
        }
        selected = null
        pinchEvent(MotionEvent.ACTION_DOWN, 40f, 0)
        pinchEvent(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 40f, 10)
        for (step in 1..24) pinchEvent(MotionEvent.ACTION_MOVE, 40f + step * 20f, 10L + step * 30)
        val afterZoom = Rect().also { provider.createAccessibilityNodeInfo(0)!!.getBoundsInParent(it) }
        assertEquals("Old layout stays fixed during pinch", beforeZoom, afterZoom)
        pinchEvent(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 520f, 750)
        pinchEvent(MotionEvent.ACTION_UP, 520f, 780)
        assertEquals("Pinch must not activate a reference", null, selected)
      }
      SystemClock.sleep(400)
      instrumentation.runOnMainSync {
        val host = view.accessibilityNodeProvider!!.createAccessibilityNodeInfo(View.NO_ID)!!
        for (index in 0 until host.childCount) {
          val bounds = Rect()
          view.accessibilityNodeProvider!!.createAccessibilityNodeInfo(index)!!.getBoundsInParent(bounds)
          assertTrue("Settled cells stay inside the viewport", bounds.left >= 0 && bounds.top >= 0 && bounds.right <= view.width && bounds.bottom <= view.height)
        }
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }

  @Test
  fun menuToggleAlignsWithTitleAndPreservesOptionsList() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    lateinit var view: LibReferenceMenuBSDView
    var toggles = 0
    try {
      instrumentation.runOnMainSync {
        view = LibReferenceMenuBSDView(activity)
        activity.addContentView(view, ViewGroup.LayoutParams(1080, ViewGroup.LayoutParams.WRAP_CONTENT))
        view.bind(buildLibReferenceMenuBottomSheetState(LibReferenceOptions.SERVICES)) {}
        view.bindDisplayMode(false) { toggles++ }
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        val list = view.getChildAt(1) as BottomSheetRecyclerView
        val button = view.findViewById<AppCompatImageButton>(R.id.lib_reference_display_mode)
        val title = view.getHeaderView().title
        val titleLocation = IntArray(2)
        val buttonLocation = IntArray(2)
        title.getLocationOnScreen(titleLocation)
        button.getLocationOnScreen(buttonLocation)
        val titleCenter = titleLocation[1] + title.paddingTop +
          (title.height - title.paddingTop - title.paddingBottom) / 2f
        assertEquals(titleCenter, buttonLocation[1] + button.height / 2f, 1f)
        val targetSize = (48 * activity.resources.displayMetrics.density).toInt()
        assertTrue(button.width >= targetSize && button.height >= targetSize)
        assertEquals(activity.getString(R.string.lib_reference_switch_to_treemap), button.contentDescription)
        val treemapIcon = button.drawable.toBitmap(48, 48)
        assertTrue(button.performClick())
        assertEquals(1, toggles)
        view.bindDisplayMode(true) { toggles++ }
        assertEquals(activity.getString(R.string.lib_reference_switch_to_list), button.contentDescription)
        assertFalse(treemapIcon.sameAs(button.drawable.toBitmap(48, 48)))
        assertTrue(button.performClick())
        assertEquals(2, toggles)
        assertSame(list, view.getChildAt(1))
        assertEquals("Treemap mode removes list demo rows immediately", 1, list.adapter!!.itemCount)
      }
      SystemClock.sleep(1000)
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        fun findTree(parent: ViewGroup): LibReferenceTreemapView? {
          for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is LibReferenceTreemapView) return child
            if (child is ViewGroup) findTree(child)?.let { return it }
          }
          return null
        }
        val tree = checkNotNull(findTree(view))
        assertTrue("Treemap mode shows the treemap demo", tree.isShown)
        val originalHeight = tree.height
        view.bind(buildLibReferenceMenuBottomSheetState(LibReferenceOptions.SERVICES or LibReferenceOptions.ACTIVITIES)) {}
        val animatorField = LibReferenceTreemapView::class.java.getDeclaredField("filterAnimator").apply { isAccessible = true }
        val animator = animatorField.get(tree) as ValueAnimator
        assertTrue("Changing demo filters must animate individual blocks", animator.isRunning)
        assertEquals(250L, animator.duration)
        animator.currentPlayTime = 100L
        view.bind(buildLibReferenceMenuBottomSheetState(LibReferenceOptions.ACTIVITIES)) {}
        val nextAnimator = animatorField.get(tree) as ValueAnimator
        assertTrue("Rapid filter changes must keep animating", nextAnimator.isRunning)
        nextAnimator.end()
        assertEquals(originalHeight, tree.height)
        view.bindDisplayMode(false) {}
        assertEquals(View.GONE, tree.visibility)
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
