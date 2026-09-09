package com.absinthe.libchecker.ui.animator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BlurCoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListReturnTopInstrumentedTest {
  @Test
  fun appbarStartsReturningBeforeListReachesTopInBothDesigns() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ) as MainActivity
    val originalBlur = GlobalValues.isBlurDesign
    try {
      for (blur in listOf(false, true)) {
        instrumentation.runOnMainSync { activity.setBlurDesignEnabled(blur) }
        SystemClock.sleep(600)
        instrumentation.runOnMainSync {
          val appbar = activity.findViewById<AppBarLayout>(R.id.appbar)
          val container = appbar.parent as BlurCoordinatorLayout
          val underlap = BlurCoordinatorLayout::class.java.getDeclaredField("contentUnderlapsAppbar").apply { isAccessible = true }
          activity.scheduleAppbarLiftingStatus(true)
          assertTrue(underlap.getBoolean(container))
          if (!blur) assertTrue(appbar.isLifted)
          activity.setAppbarReturnTopRunning(true)
          activity.scheduleAppbarLiftingStatus(true)
          assertFalse(underlap.getBoolean(container))
          assertFalse(appbar.isLifted)
          assertFalse(appbar.isLiftOnScroll)
          activity.setAppbarReturnTopRunning(false)
          activity.scheduleAppbarLiftingStatus(true)
          assertTrue(underlap.getBoolean(container))
          assertEquals(!blur, appbar.isLiftOnScroll)
        }
      }
    } finally {
      instrumentation.runOnMainSync {
        activity.setAppbarReturnTopRunning(false)
        activity.setBlurDesignEnabled(originalBlur)
      }
    }
  }

  @Test
  fun outgoingFadeAndIncomingSettleOverlapAndCancellationRestoresList() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = instrumentation.targetContext
      for (manager in listOf(LinearLayoutManager(context), StaggeredGridLayoutManager(2, RecyclerView.VERTICAL))) {
        val list = RecyclerView(context).apply {
          layoutManager = manager
          adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = 100
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(
              View(context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80.dp)
              }
            ) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
              holder.itemView.setBackgroundColor(if (position < 20) Color.BLUE else Color.RED)
            }
          }
        }
        val host = FrameLayout(context).apply { addView(list) }
        fun layout() {
          host.measure(
            View.MeasureSpec.makeMeasureSpec(400.dp, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600.dp, View.MeasureSpec.EXACTLY)
          )
          host.layout(0, 0, host.measuredWidth, host.measuredHeight)
        }
        layout()
        list.scrollToPosition(40)
        layout()
        fun color(): Int {
          val bitmap = Bitmap.createBitmap(list.width, list.height, Bitmap.Config.ARGB_8888)
          host.draw(Canvas(bitmap))
          val color = bitmap.getPixel(list.width / 4, list.height / 2)
          bitmap.recycle()
          return color
        }
        assertEquals(Color.RED, color())
        var ended = false
        val animator = list.createReturnTopAnimator { ended = true }
        animator.start()
        layout()
        assertTrue(list.canScrollVertically(-1))
        assertEquals(Color.RED, color())
        animator.setCurrentFraction(0.2f)
        assertEquals(0f, list.alpha)
        assertEquals(0f, list.translationY)
        val first = list.getChildAt(0)
        assertEquals(-32.dp, first.top)
        animator.setCurrentFraction(0.5f)
        val middle = color()
        assertTrue(Color.red(middle) > 0)
        assertTrue(Color.blue(middle) > 0)
        assertTrue(list.alpha in 0.01f..0.99f)
        val enteringY = first.top
        animator.setCurrentFraction(0.8f)
        assertTrue(first.top > enteringY)
        assertTrue(first.top < 0)
        assertEquals(0f, list.translationY)
        assertTrue(list.alpha in 0.01f..0.99f)
        assertTrue(list.canScrollVertically(-1))
        animator.end()
        layout()
        assertFalse(list.canScrollVertically(-1))
        assertEquals(0, list.getChildAt(0).top)
        assertEquals(Color.BLUE, color())
        assertTrue(ended)

        ended = false
        list.scrollToPosition(40)
        layout()
        val cancelled = list.createReturnTopAnimator { ended = true }
        cancelled.start()
        cancelled.setCurrentFraction(0.35f)
        layout()
        cancelled.cancel()
        assertEquals(Color.BLUE, color())
        assertEquals(1f, list.alpha)
        assertEquals(0f, list.translationY)
        assertTrue(ended)
      }
    }
  }
}
