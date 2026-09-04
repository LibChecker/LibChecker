package com.absinthe.libchecker.domain.home

import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.data.home.LocalRecentVisitsRepository
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.home.recent.RecentVisitsRepository
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitGroup
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitItem
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitsPopup
import com.absinthe.libchecker.utils.OsUtils
import com.google.android.material.navigation.NavigationBarView
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class RecentVisitsInstrumentedTest {
  @Test
  fun historyIsBoundedDeduplicatedAndRestoredWithoutMixingDestinations() = runBlocking {
    val preferences = InstrumentationRegistry.getInstrumentation().context
      .getSharedPreferences("recent_visits_test", Context.MODE_PRIVATE)
    preferences.edit(commit = true) { clear() }
    try {
      val store = LocalRecentVisitsRepository { preferences }
      repeat(7) { store.record(RecentVisit("app.$it")) }
      store.record(RecentVisit("app.3"))
      val provider = RecentVisit("provider", PROVIDER, "Provider", listOf("app.3|provider"))
      store.record(provider)
      store.record(RecentVisit("provider", DEX))
      store.record(RecentVisit(" "))
      store.record(RecentVisit("invalid", -9))
      val restored = LocalRecentVisitsRepository { preferences }
      assertEquals(listOf("app.3", "app.6", "app.5", "app.4", "app.2"), restored.recent(false).map { it.name })
      assertEquals(listOf(RecentVisit("provider", DEX), provider), restored.recent(true))
      repeat(7) { restored.pin(RecentVisit("app.$it")) }
      restored.pin(RecentVisit("app.3"))
      restored.pin(provider)
      restored.pin(RecentVisit("provider", DEX))
      restored.pin(RecentVisit("invalid", -9))
      val pinsRestored = LocalRecentVisitsRepository { preferences }
      assertEquals(7, pinsRestored.pinned(false).size)
      assertEquals("app.3", pinsRestored.pinned(false).first().name)
      assertEquals(listOf(RecentVisit("provider", DEX), provider), pinsRestored.pinned(true))
      assertEquals(listOf(RecentVisit("provider", DEX), provider), pinsRestored.recent(true))
      assertEquals(5, pinsRestored.recent(false).size)
      val revisionBeforeRemoval = restored.revision.value
      restored.remove(RecentVisit("provider", DEX))
      assertEquals("Removing both sections must publish one state change", revisionBeforeRemoval + 1, restored.revision.value)
      assertEquals(listOf(provider), restored.recent(true))
      assertEquals(listOf(provider), restored.pinned(true))
      val afterRemoval = restored.revision.value
      restored.remove(RecentVisit("provider", DEX))
      assertEquals("Removing an absent item must not publish another state", afterRemoval, restored.revision.value)
      restored.remove(RecentVisit("app.3"))
      assertTrue(restored.recent(false).none { it.name == "app.3" })
      assertTrue(restored.pinned(false).none { it.name == "app.3" })
      val afterRestart = LocalRecentVisitsRepository { preferences }
      assertTrue(afterRestart.recent(false).none { it.name == "app.3" })
      assertTrue(afterRestart.pinned(false).none { it.name == "app.3" })
      restored.remove(provider)
      assertTrue(restored.recent(true).isEmpty())
      preferences.edit { putString("recent_apps_v1", "{corrupt") }
      assertTrue(restored.recent(false).isEmpty())
      restored.record(RecentVisit("recovered"))
      assertEquals(listOf(RecentVisit("recovered")), restored.recent(false))
    } finally {
      preferences.edit(commit = true) { clear() }
    }
  }

  @Test
  fun longPressKeepsTheSelectedTabAndBackOrInterruptedAnimationCleansUp() {
    checkPopup(blurDesign = false)
  }

  @Test
  fun popupKeepsNavigationSharpWithProgressiveBarBlur() {
    if (OsUtils.atLeastT()) checkPopup(blurDesign = true)
  }

  @Test
  fun sideNavigationKeepsTheTailOnItsIconWhileResizing() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    var activity: MainActivity? = null
    try {
      assertTrue(instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90))
      instrumentation.targetContext.startActivity(
        Intent(instrumentation.targetContext, MainActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      )
      val main = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as MainActivity
      activity = main
      assertTrue(waitUntil { main.findViewById<View>(R.id.nav_view).let { it.height > it.width && it.width > 0 } })
      fun bounds(view: View): Rect {
        val location = IntArray(2).also(view::getLocationOnScreen)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
      }
      for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
        instrumentation.runOnMainSync { main.window.decorView.layoutDirection = direction }
        assertTrue(
          waitUntil {
            val nav = bounds(main.findViewById(R.id.nav_view))
            (nav.centerX() < main.window.decorView.width / 2) == (direction == View.LAYOUT_DIRECTION_LTR)
          }
        )
        for (tab in listOf(R.id.navigation_app_list, R.id.navigation_classify)) {
          lateinit var popup: RecentVisitsPopup
          instrumentation.runOnMainSync {
            main.findViewById<View>(tab).performLongClick()
            popup = main.popup()
          }
          assertTrue(waitUntil { popup.field("progress") == 1f })
          val surface = popup.field("surface") as View
          fun checkAnchor() {
            val nav = bounds(main.findViewById(R.id.nav_view))
            val anchor = main.findViewById<View>(tab)
            val icon = bounds(anchor.findViewById(com.google.android.material.R.id.navigation_bar_item_icon_view))
            val balloon = bounds(surface)
            val leftTail = direction == View.LAYOUT_DIRECTION_LTR
            assertTrue("Popup overlaps its navigation rail", if (leftTail) balloon.left >= nav.right else balloon.right <= nav.left)
            val edgeX = if (leftTail) 1 else surface.width - 2
            val centerY = icon.centerY() - balloon.top
            val region = Region().apply {
              setPath(surface.field("path") as Path, Region(0, 0, surface.width, surface.height))
            }
            assertTrue("Side tail does not point at the tab icon", region.contains(edgeX, centerY))
            assertFalse("The body extends into the tail gutter", region.contains(edgeX, (centerY + surface.height / 3).coerceAtMost(surface.height - 1)))
          }
          try {
            instrumentation.runOnMainSync { checkAnchor() }
            for (count in listOf(1, 5, 2)) {
              instrumentation.runOnMainSync {
                popup.updateItems(
                  RecentVisitGroup(
                    pinned = List(count) {
                      RecentVisitItem(RecentVisit("layout.$it"), "Layout $it", ColorDrawable(Color.WHITE))
                    }
                  )
                )
              }
              assertTrue(waitUntil { surface.height == (popup.field("surfaceBounds") as Rect).height() })
              instrumentation.runOnMainSync { checkAnchor() }
            }
          } finally {
            instrumentation.runOnMainSync { popup.dismissImmediately() }
          }
        }
      }
    } finally {
      instrumentation.runOnMainSync { activity?.finish() }
      instrumentation.removeMonitor(monitor)
      instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }
  }

  @Test
  fun listHoldDragPinsBothKindsAndOutsideReleaseDoesNotPin() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    instrumentation.targetContext.startActivity(
      Intent(instrumentation.targetContext, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
    )
    val activity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as MainActivity
    val repository = GlobalContext.get().get<RecentVisitsRepository>()
    val originalPins = runBlocking { listOf(repository.pinned(false), repository.pinned(true)) }
    val originalRecent = runBlocking { listOf(repository.recent(false), repository.recent(true)) }
    try {
      for (libraries in listOf(false, true)) {
        assertTrue(waitUntil { activity.findViewById<View>(R.id.navigation_classify)?.isShown == true })
        instrumentation.runOnMainSync {
          activity.findViewById<View>(if (libraries) R.id.navigation_classify else R.id.navigation_app_list).performClick()
        }
        assertTrue(waitUntil { activity.field("isPageTransitionRunning") == false })
        assertTrue(waitUntil { activity.window.decorView.visibleList()?.childCount?.let { it > 1 } == true })
        SystemClock.sleep(1000)
        val listBounds = Rect()
        instrumentation.runOnMainSync { checkNotNull(activity.window.decorView.visibleList()).getGlobalVisibleRect(listBounds) }
        val scrollDown = SystemClock.uptimeMillis()
        val scrollX = listBounds.centerX().toFloat()
        val scrollY = listBounds.centerY().toFloat()
        injectPointer(scrollDown, MotionEvent.ACTION_DOWN, scrollX, scrollY)
        for (frame in 1..8) {
          injectPointer(scrollDown, MotionEvent.ACTION_MOVE, scrollX, scrollY - frame * 20f)
          SystemClock.sleep(16)
        }
        SystemClock.sleep(850)
        assertFalse((activity.field("recentVisitsPopup") as? RecentVisitsPopup)?.isShowing == true)
        injectPointer(scrollDown, MotionEvent.ACTION_UP, scrollX, scrollY - 160f)
        instrumentation.runOnMainSync {
          val list = checkNotNull(activity.window.decorView.visibleList())
          assertTrue(list.computeVerticalScrollOffset() > 0)
          list.stopScroll()
          list.scrollToPosition(0)
          activity.showNavigationView()
        }
        SystemClock.sleep(400)
        for (accept in listOf(false, true)) {
          assertTrue(waitUntil { activity.hasWindowFocus() })
          val before = runBlocking { repository.pinned(libraries) }
          val rowBounds = Rect()
          var source: View? = null
          instrumentation.runOnMainSync {
            val list = checkNotNull(activity.window.decorView.visibleList())
            source = list.getChildAt(0)
            checkNotNull(source).getGlobalVisibleRect(rowBounds)
          }
          val fromX = rowBounds.centerX().toFloat()
          val fromY = rowBounds.centerY().toFloat()
          val downTime = SystemClock.uptimeMillis()
          fun pointer(action: Int, x: Float, y: Float) {
            injectPointer(downTime, action, x, y)
          }
          pointer(MotionEvent.ACTION_DOWN, fromX, fromY)
          SystemClock.sleep(500)
          assertFalse((activity.field("recentVisitsPopup") as? RecentVisitsPopup)?.isShowing == true)
          assertTrue("Long hold did not open the shortcuts popup", waitUntil { (activity.field("recentVisitsPopup") as? RecentVisitsPopup)?.isShowing == true })
          val popup = checkNotNull(activity.popup())
          assertEquals(libraries, popup.libraries)
          val item = popup.field("draggedItem") as RecentVisitItem
          assertTrue(waitUntil { popup.field("progress") == 1f })
          val targetBounds = Rect()
          instrumentation.runOnMainSync { (popup.field("surface") as View).getGlobalVisibleRect(targetBounds) }
          val toX = if (accept) targetBounds.centerX().toFloat() else fromX
          val toY = if (accept) targetBounds.top + targetBounds.height() * .2f else fromY
          for (frame in 1..24) {
            val fraction = frame / 24f
            pointer(MotionEvent.ACTION_MOVE, fromX + (toX - fromX) * fraction, fromY + (toY - fromY) * fraction)
            SystemClock.sleep(16)
          }
          if (accept) {
            assertTrue("Popup never accepted the native drag", waitUntil { popup.field("dropHovered") == true })
            instrumentation.runOnMainSync { checkDropHighlightRetargets(popup) }
          }
          pointer(MotionEvent.ACTION_UP, toX, toY)
          assertTrue(waitUntil { checkNotNull(source).alpha == 1f })
          if (accept) {
            assertTrue(waitUntil { runBlocking { repository.pinned(libraries).any(item.visit::sameDestination) } })
            assertTrue(popup.isShowing)
            assertTrue(waitUntil { (popup.field("items") as? RecentVisitGroup)?.pinned?.any { it.visit.sameDestination(item.visit) } == true })
            SystemClock.sleep(300)
            var pinnedRow: View? = null
            instrumentation.runOnMainSync {
              val rows = popup.field("rows") as ViewGroup
              pinnedRow = (0 until rows.childCount).map(rows::getChildAt).first { it.contentDescription == item.label }
              assertTrue(
                checkNotNull(pinnedRow).createAccessibilityNodeInfo().actionList.any {
                  it.id == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK
                }
              )
            }
            checkHoldRemoval(popup, checkNotNull(pinnedRow))
            assertTrue(waitUntil { runBlocking { repository.pinned(libraries).none(item.visit::sameDestination) } })
            checkRecentActions(popup, item, repository)
            instrumentation.runOnMainSync { popup.dismissImmediately() }
            assertTrue("Particle overlay was not cleaned up on dismissal", waitUntil { checkNotNull(pinnedRow).alpha == 1f })
          } else {
            assertTrue(waitUntil { !popup.isShowing })
            assertEquals(before, runBlocking { repository.pinned(libraries) })
          }
          println("PIN_DRAG libraries=$libraries accepted=$accept passed")
        }
      }
    } finally {
      val now = SystemClock.uptimeMillis()
      MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0).apply {
        source = InputDevice.SOURCE_TOUCHSCREEN
        instrumentation.uiAutomation.injectInputEvent(this, true)
        recycle()
      }
      instrumentation.runOnMainSync { (activity.field("recentVisitsPopup") as? RecentVisitsPopup)?.dismissImmediately() }
      runBlocking {
        for (libraries in listOf(false, true)) {
          (repository.pinned(libraries) + repository.recent(libraries)).forEach { repository.remove(it) }
          originalPins[if (libraries) 1 else 0].asReversed().forEach { repository.pin(it) }
          originalRecent[if (libraries) 1 else 0].asReversed().forEach { repository.record(it) }
        }
      }
      instrumentation.runOnMainSync { activity.finish() }
      instrumentation.removeMonitor(monitor)
    }
  }

  private fun View.visibleList(): RecyclerView? {
    if (!isShown) return null
    if (this is RecyclerView && id == android.R.id.list) return this
    if (this is ViewGroup) {
      for (index in 0 until childCount) getChildAt(index).visibleList()?.let { return it }
    }
    return null
  }

  private fun injectPointer(downTime: Long, action: Int, x: Float, y: Float) {
    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    event.source = InputDevice.SOURCE_TOUCHSCREEN
    try {
      assertTrue(InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true))
    } finally {
      event.recycle()
    }
  }

  private fun checkPopup(blurDesign: Boolean) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
    instrumentation.targetContext.startActivity(
      Intent(instrumentation.targetContext, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
    )
    val activity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000L) as MainActivity
    var popup: RecentVisitsPopup? = null
    val navigationBounds = Rect()
    try {
      instrumentation.runOnMainSync {
        activity.setFloatingNavBarEnabled(true)
        activity.setBlurDesignEnabled(blurDesign)
      }
      assertTrue(
        waitUntil {
          activity.findViewById<View>(R.id.navigation_classify)?.let { it.isShown && it.width > 0 } == true
        }
      )
      assertTrue(
        waitUntil {
          activity.findViewById<RecyclerView>(android.R.id.list)?.let { it.isShown && it.childCount > 0 } == true
        }
      )
      // Let the navigation's initial reveal and active-indicator animations settle.
      SystemClock.sleep(1000)
      instrumentation.runOnMainSync {
        val nav = activity.findViewById<View>(R.id.nav_view)
        val location = IntArray(2).also(nav::getLocationOnScreen)
        navigationBounds.set(location[0], location[1], location[0] + nav.width, location[1] + nav.height)
      }
      val baseline = checkNotNull(instrumentation.uiAutomation.takeScreenshot()).copy(Bitmap.Config.ARGB_8888, false)
      var selectedItemId = 0
      instrumentation.runOnMainSync {
        val nav = activity.findViewById<NavigationBarView>(R.id.nav_view)
        selectedItemId = nav.selectedItemId
        for (index in 0 until nav.menu.size()) {
          if (OsUtils.atLeastO()) {
            assertNull(nav.findViewById<View>(nav.menu.getItem(index).itemId).tooltipText)
          }
        }
        assertTrue(activity.findViewById<View>(R.id.navigation_classify).performLongClick())
        popup = activity.popup()
        assertTrue(checkNotNull(popup).libraries)
        assertEquals(selectedItemId, nav.selectedItemId)
      }
      assertTrue(waitUntil { (checkNotNull(popup).field("progress") as Float) == 1f })
      instrumentation.runOnMainSync {
        val host = checkNotNull(popup).field("host") as View
        val hostOrigin = IntArray(2).also(host::getLocationOnScreen)
        val popupOrigin = IntArray(2).also(checkNotNull(popup).contentView::getLocationOnScreen)
        assertEquals("Backdrop is clipped below the status bar", hostOrigin[1], popupOrigin[1])
        assertEquals("Backdrop does not cover the full host", host.height, checkNotNull(popup).contentView.height)
      }
      val opened = checkNotNull(instrumentation.uiAutomation.takeScreenshot()).copy(Bitmap.Config.ARGB_8888, false)
      assertNavigationUnchanged(baseline, opened, navigationBounds)
      instrumentation.runOnMainSync {
        val nav = activity.findViewById<NavigationBarView>(R.id.nav_view)
        val anchor = activity.findViewById<View>(R.id.navigation_classify)
        val downTime = SystemClock.uptimeMillis()
        fun dispatch(action: Int, x: Float) {
          MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, nav.height / 2f, 0).also {
            nav.dispatchTouchEvent(it)
            it.recycle()
          }
        }
        dispatch(MotionEvent.ACTION_DOWN, anchor.left + anchor.width / 2f)
        assertTrue(anchor.performLongClick())
        dispatch(MotionEvent.ACTION_MOVE, nav.width - 1f)
        dispatch(MotionEvent.ACTION_UP, nav.width - 1f)
        assertEquals(selectedItemId, nav.selectedItemId)
      }
      if (OsUtils.atLeastS()) {
        val sharpEnergy = backgroundEdgeEnergy(baseline)
        val blurredEnergy = backgroundEdgeEnergy(opened)
        assertTrue("Background edge energy: $sharpEnergy -> $blurredEnergy", sharpEnergy > 0 && blurredEnergy < sharpEnergy * .35)
        checkBlurTransition(checkNotNull(popup), sharpEnergy)
      }
      checkResizeTransition(checkNotNull(popup))
      baseline.recycle()
      opened.recycle()
      instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
      assertTrue(waitUntil { !checkNotNull(popup).isShowing })
      instrumentation.runOnMainSync {
        assertEquals(0f, checkNotNull(popup).field("progress"))
        assertFalse(activity.isFinishing)
        repeat(3) {
          activity.findViewById<View>(R.id.navigation_app_list).performLongClick()
          popup = activity.popup()
          checkNotNull(popup).dismissImmediately()
          assertFalse(checkNotNull(popup).isShowing)
          assertEquals(0f, checkNotNull(popup).field("progress"))
        }
        activity.findViewById<View>(R.id.navigation_app_list).performLongClick()
        popup = activity.popup()
        val interrupted = checkNotNull(popup)
        interrupted.contentView.doOnLayout { interrupted.dismissImmediately() }
      }
      assertTrue(waitUntil { !checkNotNull(popup).isShowing })
      // A PixelCopy callback arriving after dismissal must not restart the animation.
      SystemClock.sleep(250)
      instrumentation.runOnMainSync { assertEquals(0f, checkNotNull(popup).field("progress")) }
    } finally {
      instrumentation.runOnMainSync {
        popup?.dismissImmediately()
        activity.finish()
      }
      instrumentation.removeMonitor(monitor)
    }
  }

  private fun waitUntil(condition: () -> Boolean): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = SystemClock.uptimeMillis() + 10_000
    while (SystemClock.uptimeMillis() < deadline) {
      var done = false
      instrumentation.runOnMainSync { done = condition() }
      if (done) return true
      SystemClock.sleep(30)
    }
    return false
  }

  private fun MainActivity.popup() = field("recentVisitsPopup") as RecentVisitsPopup

  private fun checkResizeTransition(popup: RecentVisitsPopup) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val original = popup.field("items") as? RecentVisitGroup
    val surface = popup.field("surface") as View
    val samples = mutableListOf<Pair<Int, Int>>()
    val callback = object : Choreographer.FrameCallback {
      override fun doFrame(frameTimeNanos: Long) {
        samples += surface.height to surface.bottom
        Choreographer.getInstance().postFrameCallback(this)
      }
    }
    fun render(count: Int) {
      instrumentation.runOnMainSync {
        popup.updateItems(
          RecentVisitGroup(
            pinned = List(count) {
              RecentVisitItem(RecentVisit("resize.$it"), "Resize $it", ColorDrawable(Color.WHITE))
            }
          )
        )
      }
    }
    try {
      render(1)
      SystemClock.sleep(350L)
      var smallHeight = 0
      var bottom = 0
      instrumentation.runOnMainSync {
        smallHeight = surface.height
        bottom = surface.bottom
        Choreographer.getInstance().postFrameCallback(callback)
      }
      render(4)
      SystemClock.sleep(70L)
      // The repository can emit the same items again while the height is still changing.
      render(4)
      SystemClock.sleep(40L)
      instrumentation.runOnMainSync {
        val rows = popup.field("rows") as ViewGroup
        assertTrue("Rows stayed blank during resize", (0 until rows.childCount).all { rows.getChildAt(it).height > 0 })
      }
      assertTrue(waitUntil { surface.height > smallHeight && surface.height == (popup.field("surfaceBounds") as Rect).height() })
      instrumentation.runOnMainSync {
        assertTrue("Height jumped without intermediate frames: $samples", samples.any { it.first > smallHeight && it.first < surface.height })
      }
      // Reverse an in-flight resize twice; stale transitions must not restore older bounds.
      render(1)
      SystemClock.sleep(70L)
      render(5)
      SystemClock.sleep(70L)
      render(1)
      assertTrue(waitUntil { surface.height == smallHeight && surface.layoutParams.height == smallHeight })
      SystemClock.sleep(250L)
      instrumentation.runOnMainSync {
        assertEquals(smallHeight, surface.height)
        assertTrue("Resize moved the bottom anchor: $samples", samples.all { abs(it.second - bottom) <= 1 })
      }
      render(4)
      assertTrue(waitUntil { surface.height == (popup.field("surfaceBounds") as Rect).height() && surface.height > smallHeight })
      val removedBounds = Rect()
      instrumentation.runOnMainSync {
        val row = (popup.field("rows") as ViewGroup).getChildAt(1)
        // A distinctive snapshot makes a lingering solid row distinguishable from sparse particles.
        row.setBackgroundColor(Color.MAGENTA)
        val location = IntArray(2).also(row::getLocationOnScreen)
        removedBounds.set(location[0], location[1], location[0] + row.width, location[1] + row.height)
        assertTrue(row.performLongClick())
        assertFalse(row.isAttachedToWindow)
      }
      SystemClock.sleep(260L)
      val removalFrame = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
      try {
        var solidPixels = 0
        for (y in removedBounds.top until removedBounds.bottom) {
          for (x in removedBounds.left until removedBounds.right) {
            val color = removalFrame.getPixel(x, y)
            if (Color.red(color) > Color.green(color) + 96 && Color.blue(color) > Color.green(color) + 96) solidPixels++
          }
        }
        val coverage = solidPixels.toFloat() / (removedBounds.width() * removedBounds.height())
        println("REMOVAL_SNAPSHOT coverage=$coverage")
        assertTrue("Solid removed row remained after resize: coverage=$coverage", coverage < .55f)
      } finally {
        removalFrame.recycle()
      }
    } finally {
      instrumentation.runOnMainSync {
        Choreographer.getInstance().removeFrameCallback(callback)
        popup.updateItems(original)
      }
    }
  }

  private fun checkBlurTransition(popup: RecentVisitsPopup, sharpEnergy: Long) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val applyProgress = popup.javaClass.getDeclaredMethod("applyProgress", Float::class.javaPrimitiveType).apply { isAccessible = true }
    var previous: FloatArray? = null
    var largestChange = 0.0
    // Small radius changes near the end exposed the sampling jitter; also exercise reversal.
    for (step in (80..100) + (99 downTo 80)) {
      val progress = step / 100f
      instrumentation.runOnMainSync { applyProgress.invoke(popup, progress) }
      instrumentation.waitForIdleSync()
      SystemClock.sleep(50)
      val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
      val frame = screenshot.copy(Bitmap.Config.ARGB_8888, false)
      screenshot.recycle()
      try {
        assertTrue("Blur disappeared at $progress", backgroundEdgeEnergy(frame) < sharpEnergy * .35)
        val columns = (frame.width - 40) / 8
        val rows = (frame.height / 3 - frame.height / 8) / 8
        val samples = FloatArray(columns * rows) { index ->
          val x = 20 + index % columns * 8
          val y = frame.height / 8 + index / columns * 8
          // Average 4x4 pixels so sub-pixel dithering is not mistaken for moving contours.
          var sum = 0
          for (dy in 0..3) {
            for (dx in 0..3) {
              val color = frame.getPixel(x + dx, y + dy)
              sum += (color shr 16 and 255) + (color shr 8 and 255) + (color and 255)
            }
          }
          sum / 48f
        }
        previous?.let { before ->
          val change = samples.indices.sumOf { abs(samples[it] - before[it]).toDouble() } / samples.size
          largestChange = maxOf(largestChange, change)
          assertTrue("Backdrop jumped at $progress: mean change=$change", change < .35)
        }
        previous = samples
      } finally {
        frame.recycle()
      }
    }
    println("BLUR_CONTINUITY largestChange=$largestChange")
  }

  private fun assertNavigationUnchanged(before: Bitmap, after: Bitmap, bounds: Rect) {
    var difference = 0L
    var samples = 0
    val verticalInset = bounds.height() / 4
    for (y in bounds.top + verticalInset until minOf(bounds.bottom, before.height) - verticalInset step 2) {
      for (x in bounds.left + 4 until minOf(bounds.right, before.width) - 4 step 2) {
        val a = before.getPixel(x, y)
        val b = after.getPixel(x, y)
        for (shift in 0..16 step 8) {
          difference += abs((a shr shift and 255) - (b shr shift and 255))
          samples++
        }
      }
    }
    val meanDifference = difference.toDouble() / samples.coerceAtLeast(1)
    assertTrue("Popup changed navigation pixels by $meanDifference", samples > 0 && meanDifference < 1.0)
  }

  private fun backgroundEdgeEnergy(bitmap: Bitmap): Long {
    var energy = 0L
    for (y in bitmap.height / 8 until bitmap.height / 3 step 4) {
      for (x in 20 until bitmap.width - 20 step 4) {
        val current = bitmap.getPixel(x, y) and 255
        energy += abs(current - (bitmap.getPixel(x + 4, y) and 255))
        energy += abs(current - (bitmap.getPixel(x, y + 4) and 255))
      }
    }
    return energy
  }

  private fun Any.field(name: String): Any? = javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)

  private fun checkRecentActions(popup: RecentVisitsPopup, item: RecentVisitItem, repository: RecentVisitsRepository) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    runBlocking { repository.record(item.visit) }
    assertTrue(waitUntil { (popup.field("items") as? RecentVisitGroup)?.recent?.any { it.visit.sameDestination(item.visit) } == true })
    fun row(): View {
      val rows = popup.field("rows") as ViewGroup
      return (0 until rows.childCount).map(rows::getChildAt).first { it.contentDescription == item.label }
    }
    fun screenBounds(view: View): Rect {
      val location = IntArray(2).also(view::getLocationOnScreen)
      return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }
    for (accept in listOf(false, true)) {
      SystemClock.sleep(350L)
      var source: View? = null
      val from = Rect()
      val target = Rect()
      instrumentation.runOnMainSync {
        source = row()
        from.set(screenBounds(checkNotNull(source)))
        val surface = screenBounds(popup.field("surface") as View)
        val recent = screenBounds(popup.field("recentHeader") as View)
        target.set(surface.left, surface.top, surface.right, recent.top)
        assertTrue(checkNotNull(source).createAccessibilityNodeInfo().actionList.any { it.label == instrumentation.targetContext.getString(R.string.shortcuts_pin) })
      }
      val down = SystemClock.uptimeMillis()
      val fromX = from.centerX().toFloat()
      val fromY = from.centerY().toFloat()
      val toY = if (accept) target.centerY().toFloat() else fromY
      val toX = if (accept) target.centerX().toFloat() else fromX + from.width() * .3f
      injectPointer(down, MotionEvent.ACTION_DOWN, fromX, fromY)
      SystemClock.sleep(android.view.ViewConfiguration.getLongPressTimeout() + 250L)
      instrumentation.runOnMainSync {
        assertTrue((checkNotNull(checkNotNull(source).field("icon")).field("removalProgress") as Float) > 0f)
        assertTrue("Recent hold opened a context menu", popup.contentView.hasWindowFocus())
      }
      for (frame in 1..20) {
        val fraction = frame / 20f
        injectPointer(down, MotionEvent.ACTION_MOVE, fromX + (toX - fromX) * fraction, fromY + (toY - fromY) * fraction)
        SystemClock.sleep(16L)
      }
      assertTrue("Recent row did not start dragging", waitUntil { popup.field("draggingRecent") == true })
      instrumentation.runOnMainSync {
        popup.updateItems(popup.field("items") as? RecentVisitGroup)
        assertTrue("Refresh detached the dragged recent row", checkNotNull(source).isAttachedToWindow)
        assertEquals(0f, checkNotNull(checkNotNull(source).field("icon")).field("removalProgress"))
        assertEquals(accept, popup.field("dropHovered"))
        val preview = popup.field("dragPreview") as View
        val position = IntArray(2).also(preview::getLocationOnScreen)
        val hotspot = preview.field("touch") as PointF
        assertEquals("Drag preview did not follow horizontally", toX, position[0] + hotspot.x + (preview.field("offsetX") as Float), 2f)
        assertEquals("Drag preview did not follow vertically", toY, position[1] + hotspot.y + (preview.field("offsetY") as Float), 2f)
      }
      if (!accept) SystemClock.sleep(1300L)
      assertTrue("Dragging continued the delete countdown", checkNotNull(source).isAttachedToWindow)
      injectPointer(down, MotionEvent.ACTION_UP, toX, toY)
      assertTrue(waitUntil { popup.field("draggingRecent") == false })
      assertTrue("Internal drag closed the popup", popup.isShowing)
      assertTrue(waitUntil { runBlocking { repository.pinned(popup.libraries).any(item.visit::sameDestination) } == accept })
      assertTrue(runBlocking { repository.recent(popup.libraries).any(item.visit::sameDestination) })
      println("RECENT_DRAG libraries=${popup.libraries} accepted=$accept passed")
    }
    // Deleting a pin must also remove its hidden history, without a second layout target.
    assertTrue(waitUntil { (popup.field("items") as? RecentVisitGroup)?.pinned?.any { it.visit.sameDestination(item.visit) } == true })
    SystemClock.sleep(350L)
    val heights = mutableListOf<Int>()
    val reappeared = mutableListOf<Boolean>()
    val frame = object : Choreographer.FrameCallback {
      override fun doFrame(frameTimeNanos: Long) {
        heights += (popup.field("surface") as View).height
        val rows = popup.field("rows") as ViewGroup
        reappeared += (0 until rows.childCount).any { rows.getChildAt(it).contentDescription == item.label }
        Choreographer.getInstance().postFrameCallback(this)
      }
    }
    instrumentation.runOnMainSync {
      assertTrue(row().performLongClick())
      Choreographer.getInstance().postFrameCallback(frame)
    }
    try {
      assertTrue(
        "Deleted pin remained in recent history",
        waitUntil {
          runBlocking { repository.recent(popup.libraries).none(item.visit::sameDestination) }
        }
      )
      assertTrue(runBlocking { repository.pinned(popup.libraries).none(item.visit::sameDestination) })
      SystemClock.sleep(450L)
    } finally {
      instrumentation.runOnMainSync { Choreographer.getInstance().removeFrameCallback(frame) }
    }
    assertTrue("Deletion frames were not sampled", heights.size > 2)
    assertTrue("Deleted pin reappeared in the popup", reappeared.none { it })
    assertTrue("Popup grew again while deleting a pin", heights.zipWithNext().all { (before, after) -> after <= before })
    println("PIN_DELETE libraries=${popup.libraries} frames=${heights.size} passed")

    // A fresh visit can appear again and supports partial and complete hold removal.
    runBlocking { repository.record(item.visit) }
    assertTrue(waitUntil { (popup.field("items") as? RecentVisitGroup)?.recent?.any { it.visit.sameDestination(item.visit) } == true })
    SystemClock.sleep(350L)
    var recentRow: View? = null
    instrumentation.runOnMainSync { recentRow = row() }
    checkHoldRemoval(popup, checkNotNull(recentRow), draggable = true)
    assertTrue(waitUntil { runBlocking { repository.recent(popup.libraries).none(item.visit::sameDestination) } })
    assertTrue(runBlocking { repository.pinned(popup.libraries).none(item.visit::sameDestination) })
    println("RECENT_REMOVE libraries=${popup.libraries} passed")
  }

  private fun checkHoldRemoval(popup: RecentVisitsPopup, row: View, draggable: Boolean = false) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val bounds = Rect()
    instrumentation.runOnMainSync {
      assertTrue(row.isShown)
      val location = IntArray(2).also(row::getLocationOnScreen)
      bounds.set(location[0], location[1], location[0] + row.width, location[1] + row.height)
    }
    val icon = checkNotNull(row.field("icon"))
    fun progress() = icon.field("removalProgress") as Float
    val x = bounds.centerX().toFloat()
    val y = bounds.centerY().toFloat()
    val longPress = android.view.ViewConfiguration.getLongPressTimeout().toLong()
    for (moveAway in if (draggable) listOf(false) else listOf(false, true)) {
      val down = SystemClock.uptimeMillis()
      injectPointer(down, MotionEvent.ACTION_DOWN, x, y)
      SystemClock.sleep(longPress + 300L)
      instrumentation.runOnMainSync {
        val beforeRelease = progress()
        assertTrue(
          "Hold progress=$beforeRelease attached=${row.isAttachedToWindow} touching=${row.field("touching")} holding=${row.field("holding")} consumed=${row.field("consumedHold")} removed=${row.field("removed")} pressed=${row.isPressed} bounds=$bounds",
          beforeRelease > 0f && beforeRelease < 1f
        )
        assertTrue("A context menu stole focus", popup.contentView.hasWindowFocus())
      }
      if (moveAway) injectPointer(down, MotionEvent.ACTION_MOVE, x + row.width / 3f, y)
      injectPointer(down, MotionEvent.ACTION_UP, if (moveAway) x + row.width / 3f else x, y)
      instrumentation.runOnMainSync {
        assertTrue("Release should drain rather than jump to zero", progress() > 0f && progress() < 1f)
      }
      assertTrue("Progress did not return to zero", waitUntil { progress() == 0f })
      assertTrue("An incomplete hold removed the row", row.isAttachedToWindow)
      assertTrue("Releasing the hold opened the item", popup.isShowing)
    }
    val down = SystemClock.uptimeMillis()
    injectPointer(down, MotionEvent.ACTION_DOWN, x, y)
    SystemClock.sleep(longPress + 700L)
    assertTrue("Removal completed before the full hold", row.isAttachedToWindow)
    assertTrue("Full hold did not remove the row", waitUntil { !row.isAttachedToWindow })
    assertTrue("The row snapshot was not handed to the particle effect", waitUntil { row.alpha == 0f })
    injectPointer(down, MotionEvent.ACTION_UP, x, y)
    assertTrue(popup.isShowing)
    // Exercise both natural particle completion and dismissal while particles are active.
    if (popup.libraries) {
      assertTrue("Particle effect did not finish", waitUntil { row.alpha == 1f })
    } else {
      SystemClock.sleep(700L)
    }
  }

  private fun checkDropHighlightRetargets(popup: RecentVisitsPopup) {
    val setHovered = popup.javaClass.getDeclaredMethod("setDropHovered", Boolean::class.javaPrimitiveType).apply { isAccessible = true }
    val surface = checkNotNull(popup.field("surface"))
    fun fraction() = surface.field("dropHighlight") as Float
    fun animation() = popup.field("dropAnimator") as android.animation.ValueAnimator
    setHovered.invoke(popup, false)
    animation().end()
    assertEquals(0f, fraction())
    setHovered.invoke(popup, true)
    val entering = animation()
    entering.currentPlayTime = 64L
    val entered = fraction()
    assertTrue(entered > 0f && entered < 1f)
    setHovered.invoke(popup, false)
    assertFalse(entering.isStarted)
    assertEquals(entered, fraction())
    animation().currentPlayTime = 60L
    val leaving = fraction()
    assertTrue(leaving > 0f && leaving < entered)
    setHovered.invoke(popup, true)
    assertEquals(leaving, fraction())
    animation().end()
    assertEquals(1f, fraction())
  }
}
