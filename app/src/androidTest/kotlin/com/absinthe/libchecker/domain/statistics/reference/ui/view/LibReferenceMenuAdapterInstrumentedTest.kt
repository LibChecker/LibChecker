package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Intent
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceMenuBottomSheetState
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceMenuAdapterInstrumentedTest {
  @Test
  fun repeatedModeSwitchesKeepChildNotificationFanoutBounded() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    try {
      instrumentation.runOnMainSync {
        val view = LibReferenceMenuBSDView(activity)
        view.bind(buildLibReferenceMenuBottomSheetState(LibReferenceOptions.SERVICES)) {}
        val list = view.getChildAt(1) as BottomSheetRecyclerView
        val options = (list.adapter as ConcatAdapter).adapters.last()
        val observed = mutableSetOf<ConcatAdapter>()
        var notifications = 0
        val observer = object : RecyclerView.AdapterDataObserver() {
          override fun onChanged() {
            notifications++
          }
        }
        repeat(1000) { index ->
          val adapter = list.adapter as ConcatAdapter
          if (observed.add(adapter)) adapter.registerAdapterDataObserver(observer)
          view.bindDisplayMode(index % 2 == 0) {}
        }
        notifications = 0
        options.notifyDataSetChanged()
        assertTrue("Child updates must not notify abandoned mode adapters: $notifications", notifications <= 2)
        assertTrue("The notification check must exercise an attached child adapter", notifications > 0)
        observed.forEach { it.unregisterAdapterDataObserver(observer) }
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
