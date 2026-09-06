package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.constant.options.AdvancedOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ComponentMenuStateTest {
  @Test
  fun preservesExistingOptionsAndAllPreviewStates() {
    val state = buildComponentMenuState(AdvancedOptions.MARK_EXPORTED, false, "com.example.test")
    assertEquals(
      listOf(AdvancedOptions.MARK_EXPORTED, AdvancedOptions.MARK_DISABLED, AdvancedOptions.SHOW_MARKED_LIB),
      state.componentOptions.map { it.option }
    )
    assertEquals(listOf(true, false, false), state.componentOptions.map { it.isChecked })
    assertEquals(listOf(EXPORTED, null, DISABLED), state.componentDemoItems.map { it.item.source })
    assertFalse(state.colorfulRuleIcon)
  }
}
