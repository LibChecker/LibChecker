package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessBarRenderStateTest {

  @Test
  fun buildsOrderedItemsAndSelectsByProcessName() {
    val state = ProcessBarRenderState.create(
      processColors = linkedMapOf(
        ":main" to 11,
        ":worker" to 22
      ),
      selectedProcess = ":worker",
      processMode = true,
      hasNonGrantedPermissions = false
    )

    assertEquals(
      listOf(
        ProcessBarItemRenderState(process = ":main", color = 11, selected = false),
        ProcessBarItemRenderState(process = ":worker", color = 22, selected = true)
      ),
      state.items
    )
    assertTrue(state.visible)
  }

  @Test
  fun hidesWhenProcessModeIsOffAndPermissionFilterIsUnavailable() {
    val state = ProcessBarRenderState.create(
      processColors = mapOf(":main" to 11),
      selectedProcess = null,
      processMode = false,
      hasNonGrantedPermissions = false
    )

    assertFalse(state.visible)
  }

  @Test
  fun keepsPermissionFilterVisibleOutsideProcessMode() {
    val state = ProcessBarRenderState.create(
      processColors = mapOf("Not granted" to 33),
      selectedProcess = "Not granted",
      processMode = false,
      hasNonGrantedPermissions = true
    )

    assertTrue(state.visible)
    assertTrue(state.items.single().selected)
  }

  @Test
  fun keepsBarVisibleUntilPermissionStateIsKnown() {
    val state = ProcessBarRenderState.create(
      processColors = mapOf(":main" to 11),
      selectedProcess = null,
      processMode = false,
      hasNonGrantedPermissions = null
    )

    assertTrue(state.visible)
  }

  @Test
  fun emptyProcessDataIsNeverVisible() {
    val state = ProcessBarRenderState.create(
      processColors = emptyMap(),
      selectedProcess = null,
      processMode = true,
      hasNonGrantedPermissions = true
    )

    assertTrue(state.items.isEmpty())
    assertFalse(state.visible)
  }
}
