package com.absinthe.libchecker.view.app

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DotLoadingViewInstrumentedTest {
  @Test
  fun animationFollowsParentVisibilityLifecycleAndReattachment() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    try {
      instrumentation.runOnMainSync {
        val owner = object : LifecycleOwner {
          override val lifecycle = LifecycleRegistry(this)
        }
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        val parent = FrameLayout(activity).apply { setViewTreeLifecycleOwner(owner) }
        val loading = DotLoadingView(activity)
        parent.addView(loading)
        activity.addContentView(parent, FrameLayout.LayoutParams(600, 600))
        val animator = RingDotsView::class.java.getDeclaredField("frameAnimator").apply { isAccessible = true }
        fun running() = animator.get(loading.loadingView) != null
        assertTrue(running())
        parent.visibility = View.GONE
        assertFalse(running())
        parent.visibility = View.VISIBLE
        assertTrue(running())
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        assertFalse(running())
        loading.setProgress(60, false)
        loading.setProgress(null)
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        assertTrue(running())
        parent.removeView(loading)
        assertFalse(running())
        parent.addView(loading)
        assertTrue(running())
        assertEquals(loading.loadingView.layoutParams.width, loading.loadingView.layoutParams.height)
        parent.removeView(loading)
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
