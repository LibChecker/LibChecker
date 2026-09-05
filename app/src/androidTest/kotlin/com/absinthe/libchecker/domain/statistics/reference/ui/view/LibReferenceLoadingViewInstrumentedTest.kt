package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceLoadingViewInstrumentedTest {
  @Test
  fun postScanStagesNeverReturnToIndeterminateModeOrMoveBackwards() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val view = LibReferenceLoadingView(context)
      val indicator = view.children.filterIsInstance<CircularProgressIndicator>().single()
      assertTrue(indicator.isIndeterminate)
      val states = listOf(
        LibReferenceLoadingState.Scanning(0),
        LibReferenceLoadingState.Scanning(50),
        LibReferenceLoadingState.Scanning(100),
        LibReferenceLoadingState.Matching(),
        LibReferenceLoadingState.Matching(50),
        LibReferenceLoadingState.Matching(100),
        LibReferenceLoadingState.Organizing(),
        LibReferenceLoadingState.Organizing(50),
        LibReferenceLoadingState.Organizing(100)
      )
      var previousProgress = 0
      states.forEach { state ->
        view.bind(state)
        assertFalse("Returned to a spinner at $state", indicator.isIndeterminate)
        assertTrue("Progress moved backwards at $state", indicator.progress >= previousProgress)
        assertTrue("Completed before the list was ready", indicator.progress < 100)
        previousProgress = indicator.progress
      }
      view.bind(LibReferenceLoadingState.Preparing)
      assertTrue(indicator.isIndeterminate)
      assertEquals(context.getString(R.string.loading), view.contentDescription)
      assertFalse(view.children.any { it is TextView })
    }
  }
}
