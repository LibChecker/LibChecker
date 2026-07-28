package com.absinthe.libchecker.ui.app

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.ui.view.AlternativeLaunchBSDView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppBundleBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppPropsBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.ELFInfoBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.LibDetailBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.SignatureDetailBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.XmlBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.XposedInfoBottomSheetView
import com.absinthe.libchecker.domain.statistics.chart.ui.ClassifyDialogView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomSheetRecyclerViewInstrumentedTest {

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
