package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Intent
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolbarConnectionLoadingViewInstrumentedTest {
  @Test
  fun requestedAnimationResumesAfterReattachmentUntilExplicitlyStopped() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    try {
      instrumentation.runOnMainSync {
        val parent = FrameLayout(activity)
        activity.addContentView(parent, FrameLayout.LayoutParams(600, 600))
        val loading = ToolbarConnectionLoadingView(activity)
        val animatorField = ToolbarConnectionLoadingView::class.java.getDeclaredField("animator").apply { isAccessible = true }
        fun started() = (animatorField.get(loading) as? ValueAnimator)?.isStarted == true

        loading.start()
        parent.addView(loading, FrameLayout.LayoutParams(100, 100))
        assertTrue(started())
        repeat(3) {
          parent.removeView(loading)
          assertFalse(started())
          parent.addView(loading)
          assertTrue("Loading must resume when its footer returns", started())
        }
        loading.stop()
        assertFalse(started())
        parent.removeView(loading)
        parent.addView(loading)
        assertFalse("Explicit stop must survive reattachment", started())
        loading.start()
        assertTrue(started())
        parent.removeView(loading)
        loading.stop()
        parent.addView(loading)
        assertFalse("Completing loading while detached must prevent a restart", started())
        parent.removeView(loading)
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
