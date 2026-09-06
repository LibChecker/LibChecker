package com.absinthe.libchecker.domain.app.list.model

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMenuBottomSheetStateTest {

  @Test
  fun `builds ordered sections and checked options`() {
    val state = buildAdvancedMenuBottomSheetState(
      displayOptions = AdvancedOptions.SHOW_SYSTEM_APPS or AdvancedOptions.SHOW_TARGET_API,
      targetApi = 35
    )

    assertEquals(
      listOf(
        AdvancedOptions.SHOW_SYSTEM_APPS,
        AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS,
        AdvancedOptions.SHOW_OVERLAYS,
        AdvancedOptions.SHOW_64_BIT_APPS,
        AdvancedOptions.SHOW_32_BIT_APPS
      ),
      state.filterOptions.map { it.option }
    )
    assertEquals(
      listOf(
        AdvancedOptions.SHOW_ANDROID_VERSION,
        AdvancedOptions.SHOW_TARGET_API,
        AdvancedOptions.SHOW_MIN_API,
        AdvancedOptions.SHOW_COMPILE_API,
        AdvancedOptions.TINT_ABI_LABEL
      ),
      state.viewOptions.map { it.option }
    )
    assertTrue(state.filterOptions.first().isChecked)
    assertTrue(state.viewOptions[1].isChecked)
    assertEquals(Constants.EXAMPLE_PACKAGE, state.demoItem.packageName)
    assertEquals(35, state.demoItem.targetApi.toInt())
  }
}
