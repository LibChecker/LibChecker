package com.absinthe.libchecker.domain.app.detail.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.os.SystemClock
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.model.DetailFeatureItem
import com.absinthe.libchecker.domain.app.detail.model.FeatureItem
import com.absinthe.libchecker.domain.app.detail.model.FeatureItemIcon
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.view.app.ToolbarConnectionLoadingView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailFeatureListControllerInstrumentedTest {
  @Test
  fun loadingFooterAnimatesAfterScrollingOutAndBackDuringIncrementalLoading() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    try {
      lateinit var controller: DetailFeatureListController
      lateinit var list: RecyclerView
      lateinit var loading: ToolbarConnectionLoadingView
      val animatorField = ToolbarConnectionLoadingView::class.java.getDeclaredField("animator").apply { isAccessible = true }
      val progressField = ToolbarConnectionLoadingView::class.java.getDeclaredField("progress").apply { isAccessible = true }
      instrumentation.runOnMainSync {
        val header = FrameLayout(activity)
        activity.addContentView(header, FrameLayout.LayoutParams(400, 200))
        controller = DetailFeatureListController(header) {}
        controller.setLoading(true)
        controller.attachWithAnimation()
        list = header.children.filterIsInstance<RecyclerView>().single()
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        loading = list.children.filterIsInstance<ToolbarConnectionLoadingView>().single()
        repeat(20) {
          controller.addItem(
            DetailFeatureItem(FeatureItem(R.string.detail_label, FeatureItemIcon.Resource(R.drawable.ic_yes), AppDetailFeatureAction.AppProp))
          )
        }
      }
      fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var satisfied = false
        while (!satisfied && SystemClock.uptimeMillis() < deadline) {
          instrumentation.runOnMainSync { satisfied = condition() }
          if (!satisfied) SystemClock.sleep(20)
        }
        assertTrue(message, satisfied)
      }
      awaitCondition("Incremental features must reach the adapter") { list.adapter!!.itemCount == 21 }
      instrumentation.runOnMainSync { list.scrollToPosition(0) }
      awaitCondition("Loading footer must leave the screen") { !loading.isAttachedToWindow }
      instrumentation.runOnMainSync {
        assertFalse((animatorField.get(loading) as? ValueAnimator)?.isStarted == true)
        list.scrollToPosition(list.adapter!!.itemCount - 1)
      }
      awaitCondition("Loading footer must return") { loading.isAttachedToWindow }
      var progress = 0f
      instrumentation.runOnMainSync {
        assertTrue("Returned footer must animate", (animatorField.get(loading) as? ValueAnimator)?.isStarted == true)
        progress = progressField.getFloat(loading)
      }
      awaitCondition("Animation must advance frames after returning") { progressField.getFloat(loading) != progress }
      instrumentation.runOnMainSync {
        assertNotEquals(progress, progressField.getFloat(loading))
        controller.setLoading(false)
        assertFalse((animatorField.get(loading) as? ValueAnimator)?.isStarted == true)
      }
      awaitCondition("Completed loading must remove the footer") { !loading.isAttachedToWindow }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
