package com.absinthe.libchecker.ui.app

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AlternativeLaunchBSDView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppBundleBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppPropsBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.ELFInfoBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.LibDetailBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.SignatureDetailBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.XmlBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.XposedInfoBottomSheetView
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.statistics.chart.model.AndroidVersionLabelDisplayData
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.ChartActivity
import com.absinthe.libchecker.domain.statistics.chart.ui.ClassifyBottomSheetDialogFragment
import com.absinthe.libchecker.domain.statistics.chart.ui.ClassifyDialogView
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libchecker.view.app.TextColumnRowView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.ext.android.getViewModel

@RunWith(AndroidJUnit4::class)
class BottomSheetRecyclerViewInstrumentedTest {

  @Test
  fun sharedItemRowConsumesAvailableWidth() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val row = TextColumnRowView(context)
      row.measure(
        View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
      )

      assertEquals(720, row.measuredWidth)
    }
  }

  @Test
  fun scaffoldKeepsHeaderBeforeContentAndExposesItThroughHeaderContract() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val scaffold = BottomSheetScaffoldView(context)
      val content = View(context)

      scaffold.addContentView(content)

      assertEquals(2, scaffold.childCount)
      assertSame(scaffold.getHeaderView(), scaffold.getChildAt(0))
      assertSame(content, scaffold.getChildAt(1))
    }
  }

  @Test
  fun downwardScrollMovesSheetOnlyAfterListReachesTop() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val coordinator = CoordinatorLayout(context)
      val sheet = FrameLayout(context)
      val behavior = BottomSheetBehavior<FrameLayout>().apply {
        peekHeight = 400
        state = BottomSheetBehavior.STATE_EXPANDED
      }
      coordinator.addView(
        sheet,
        CoordinatorLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          1200
        ).apply {
          this.behavior = behavior
        }
      )

      val list = BottomSheetRecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        adapter = FixedHeightAdapter()
      }
      sheet.addView(
        list,
        FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      )

      val widthSpec = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY)
      val heightSpec = View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY)
      coordinator.measure(widthSpec, heightSpec)
      coordinator.layout(0, 0, 720, 1600)
      list.scrollBy(0, 600)
      assertTrue(list.canScrollVertically(-1))

      val accepted = behavior.onStartNestedScroll(
        coordinator,
        sheet,
        list,
        list,
        ViewCompat.SCROLL_AXIS_VERTICAL,
        ViewCompat.TYPE_TOUCH
      )
      assertTrue(accepted)

      val expandedTop = sheet.top
      val consumedWhileScrolled = intArrayOf(0, 0)
      behavior.onNestedPreScroll(
        coordinator,
        sheet,
        list,
        0,
        -100,
        consumedWhileScrolled,
        ViewCompat.TYPE_TOUCH
      )
      assertEquals(0, consumedWhileScrolled[1])
      assertEquals(expandedTop, sheet.top)

      list.scrollBy(0, -10_000)
      assertFalse(list.canScrollVertically(-1))
      val topAtListStart = sheet.top
      val consumedAtListStart = intArrayOf(0, 0)
      behavior.onNestedPreScroll(
        coordinator,
        sheet,
        list,
        0,
        -100,
        consumedAtListStart,
        ViewCompat.TYPE_TOUCH
      )
      assertEquals(-100, consumedAtListStart[1])
      assertEquals(topAtListStart + 100, sheet.top)
    }
  }

  @Test
  fun scrollableBottomSheetContentParticipatesInNestedScrolling() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val hosts = listOf(
        AlternativeLaunchBSDView(context),
        AppBundleBottomSheetView(context),
        AppPropsBottomSheetView(context) {},
        ELFInfoBottomSheetView(context),
        LibDetailBottomSheetView(context),
        SignatureDetailBottomSheetView(context),
        XmlBottomSheetView(context),
        XposedInfoBottomSheetView(context),
        ClassifyDialogView(context)
      )

      hosts.forEach { host ->
        val lists = host.findBottomSheetRecyclerViews()
        assertFalse("${host.javaClass.simpleName} has no BottomSheetRecyclerView", lists.isEmpty())
        lists.forEach { list ->
          assertTrue(
            "${host.javaClass.simpleName} must participate in BottomSheetBehavior nested scrolling",
            list.isNestedScrollingEnabled
          )
        }
      }
    }
  }

  @Test
  fun classifyDialogKeepsContentAcrossActivityRecreation() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val item = LCItem("com.example.test", "Test App", "1.0", 1L, 0L, 0L, false, 0, 0, 37, 0)
    val state = ClassifyDialogState(
      title = "Target SDK 37",
      subtitle = "Test classification",
      items = listOf(item),
      itemViewStates = emptyMap(),
      itemChips = mapOf(item.packageName to listOf("Test chip")),
      androidVersion = AndroidVersionLabelDisplayData(null, "Android 17")
    )
    val tag = ClassifyBottomSheetDialogFragment::class.java.name
    ActivityScenario.launch(ChartActivity::class.java).use { scenario ->
      instrumentation.waitForIdleSync()
      scenario.onActivity { activity ->
        activity.getViewModel<ChartViewModel>().classifyDialogState = state
        ClassifyBottomSheetDialogFragment().apply {
          showNow(activity.supportFragmentManager, tag)
        }
      }
      repeat(2) {
        scenario.recreate()
        val deadline = SystemClock.uptimeMillis() + 3000
        var rendered = false
        while (!rendered && SystemClock.uptimeMillis() < deadline) {
          instrumentation.waitForIdleSync()
          scenario.onActivity { activity ->
            val dialog = activity.supportFragmentManager.findFragmentByTag(tag) as ClassifyBottomSheetDialogFragment
            val list = dialog.root.findBottomSheetRecyclerViews().single()
            rendered = dialog.root.getHeaderView().title.text == state.title &&
              list.childCount > 0 && list.height > 0
          }
          if (!rendered) SystemClock.sleep(50)
        }
        assertTrue("Recreated dialog must retain its title and render app rows", rendered)
        scenario.onActivity { activity ->
          val dialog = activity.supportFragmentManager.findFragmentByTag(tag) as ClassifyBottomSheetDialogFragment
          assertSame(state, activity.getViewModel<ChartViewModel>().classifyDialogState)
          val adapter = dialog.root.findBottomSheetRecyclerViews().single().adapter as
            com.chad.library.adapter.base.BaseQuickAdapter<*, *>
          assertEquals(state.items, adapter.items)
        }
      }
      scenario.onActivity { activity ->
        val dialog = activity.supportFragmentManager.findFragmentByTag(tag) as ClassifyBottomSheetDialogFragment
        dialog.dismissNow()
        assertNull(activity.getViewModel<ChartViewModel>().classifyDialogState)
        ClassifyBottomSheetDialogFragment().showNow(activity.supportFragmentManager, tag)
        activity.supportFragmentManager.executePendingTransactions()
        assertNull("Without retained data the sheet must close instead of showing a false empty state", activity.supportFragmentManager.findFragmentByTag(tag))
      }
    }
  }

  @Test
  fun classifyListRemeasuresAfterAsyncRowsArrive() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      instrumentation.waitForIdleSync()
      for ((width, height) in listOf(720 to 1200, 1200 to 720)) {
        lateinit var host: FrameLayout
        lateinit var view: ClassifyDialogView
        lateinit var list: BottomSheetRecyclerView
        val committed = CountDownLatch(1)
        var initialHeight = 0
        scenario.onActivity { activity ->
          host = FrameLayout(activity)
          activity.addContentView(host, FrameLayout.LayoutParams(width, height))
          view = ClassifyDialogView(activity)
          host.addView(view, FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT))
          list = view.findBottomSheetRecyclerViews().single()
          list.adapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
              committed.countDown()
            }
          })
          view.bind(
            ClassifyDialogState(
              title = "Target SDK 37",
              subtitle = null,
              items = listOf(LCItem("com.example.test", "Test App", "1.0", 1L, 0L, 0L, false, 0, 0, 37, 0)),
              itemViewStates = emptyMap(),
              androidVersion = null
            )
          ) {}
          // Force the first layout before the asynchronous diff can commit on the main thread.
          host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
          )
          host.layout(0, 0, width, height)
          initialHeight = view.height
          assertEquals(0, list.adapter!!.itemCount)
        }
        assertTrue("Rows must commit", committed.await(5, TimeUnit.SECONDS))
        val deadline = SystemClock.uptimeMillis() + 3000
        var expanded = false
        while (!expanded && SystemClock.uptimeMillis() < deadline) {
          instrumentation.waitForIdleSync()
          scenario.onActivity { expanded = view.height > initialHeight && list.height > 0 && list.childCount > 0 }
          if (!expanded) SystemClock.sleep(50)
        }
        scenario.onActivity {
          try {
            assertEquals(1, list.adapter!!.itemCount)
            assertTrue("Rows must grow the sheet after its empty first layout ($width x $height)", expanded)
          } finally {
            (host.parent as ViewGroup).removeView(host)
          }
        }
      }
    }
  }

  private fun View.findBottomSheetRecyclerViews(): List<BottomSheetRecyclerView> {
    val result = mutableListOf<BottomSheetRecyclerView>()
    if (this is BottomSheetRecyclerView) {
      result += this
    }
    if (this is ViewGroup) {
      for (index in 0 until childCount) {
        result += getChildAt(index).findBottomSheetRecyclerViews()
      }
    }
    return result
  }

  private class FixedHeightAdapter : RecyclerView.Adapter<FixedHeightViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FixedHeightViewHolder {
      return FixedHeightViewHolder(
        View(parent.context).apply {
          layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            100
          )
        }
      )
    }

    override fun onBindViewHolder(holder: FixedHeightViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = 100
  }

  private class FixedHeightViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
