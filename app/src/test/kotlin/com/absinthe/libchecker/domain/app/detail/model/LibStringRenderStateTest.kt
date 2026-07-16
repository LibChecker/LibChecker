package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibStringRenderStateTest {

  @Test
  fun `updates highlight position without losing shared render inputs`() {
    val state = LibStringRenderState(
      itemDisplayOptions = 7,
      colorfulRuleIcon = false,
      highlightText = "sample",
      processMode = true,
      processColors = mapOf("sample.process" to 42),
      highlightPosition = 3
    )

    val updated = state.withHighlightPosition(8)

    assertEquals(7, updated.itemDisplayOptions)
    assertFalse(updated.colorfulRuleIcon)
    assertEquals("sample", updated.highlightText)
    assertTrue(updated.processMode)
    assertEquals(mapOf("sample.process" to 42), updated.processColors)
    assertEquals(8, updated.highlightPosition)
  }

  @Test
  fun `ignores invalid highlight position`() {
    val state = LibStringRenderState(highlightPosition = 3)

    assertSame(state, state.withHighlightPosition(-1))
  }
}
