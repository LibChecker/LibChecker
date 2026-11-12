package com.absinthe.libchecker.ui.app

import android.content.res.Configuration
import android.graphics.Color
import android.widget.HorizontalScrollView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.list.ui.view.AdvancedMenuSortView
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.app.CheckableChipView
import com.google.android.material.R as MaterialR
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

  @Test
  fun sortToggleMatchesDynamicThemeChipColors() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
      }
      val baseContext = ContextThemeWrapper(
        instrumentation.targetContext.createConfigurationContext(config),
        R.style.AppTheme
      )
      val dynamicContext = DynamicColors.wrapContextIfAvailable(
        baseContext,
        DynamicColorsOptions.Builder().setContentBasedSource(Color.rgb(0, 150, 136)).build()
      )
      val chip = CheckableChipView(dynamicContext).apply { isChecked = true }
      val sortView = AdvancedMenuSortView(dynamicContext).apply {
        bind(AdvancedOptions.SORT_BY_NAME) {}
      }
      val toggleGroup = (sortView.getChildAt(1) as HorizontalScrollView).getChildAt(0) as MaterialButtonToggleGroup
      val checkedButton = toggleGroup.getChildAt(0) as MaterialButton
      val uncheckedButton = toggleGroup.getChildAt(1) as MaterialButton
      val checkedState = intArrayOf(android.R.attr.state_checkable, android.R.attr.state_checked, android.R.attr.state_enabled)
      val uncheckedState = intArrayOf(android.R.attr.state_checkable, android.R.attr.state_enabled)

      assertNotEquals(baseContext.getColorByAttr(MaterialR.attr.colorSurfaceInverse), chip.checkedColor)
      assertEquals(chip.checkedColor, checkedButton.backgroundTintList?.getColorForState(checkedState, 0))
      assertEquals(chip.checkedTextColor, checkedButton.textColors.getColorForState(checkedState, 0))
      assertEquals(chip.defaultTextColor, uncheckedButton.textColors.getColorForState(uncheckedState, 0))
      assertEquals(chip.outlineColor, uncheckedButton.strokeColor?.getColorForState(uncheckedState, 0))
    }
  }

  private fun uncheckedItem() = MenuOptionItem(
    labelRes = android.R.string.ok,
    option = 1,
    isChecked = false
  )

  private fun checkedItem() = uncheckedItem().copy(isChecked = true)
}
