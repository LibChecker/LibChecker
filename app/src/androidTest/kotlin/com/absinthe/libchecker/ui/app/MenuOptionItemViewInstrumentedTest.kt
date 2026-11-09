package com.absinthe.libchecker.ui.app

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.app.CheckableChipView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuOptionItemViewInstrumentedTest {

  @Test
  fun immediateDispatchCommitsBeforeAnimationAndRebindKeepsChip() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val itemView = MenuOptionItemView(
        context = context,
        dispatchCheckedChangeImmediately = true
      )
      var committedState: Boolean? = null
      itemView.bind(uncheckedItem()) { committedState = it }
      val chip = itemView.getChildAt(0) as CheckableChipView

      chip.performClick()

      assertEquals(true, committedState)
      assertTrue(chip.isChecked)
      itemView.bind(checkedItem()) {}
      assertSame(chip, itemView.getChildAt(0))
    }
  }

  @Test
  fun defaultDispatchRemainsTiedToAnimationCompletion() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val completion = CountDownLatch(1)

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val itemView = MenuOptionItemView(context)
      itemView.bind(uncheckedItem()) { completion.countDown() }

      (itemView.getChildAt(0) as CheckableChipView).performClick()

      assertFalse(completion.await(0, TimeUnit.MILLISECONDS))
    }

    assertTrue(completion.await(2, TimeUnit.SECONDS))
  }

  private fun uncheckedItem() = MenuOptionItem(
    labelRes = android.R.string.ok,
    option = 1,
    isChecked = false
  )

  private fun checkedItem() = uncheckedItem().copy(isChecked = true)
}
