package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDetailToolbarStateTest {

  @Test
  fun defaultStateHasNoVisibleActions() {
    assertEquals(emptyList<AppDetailToolbarAction>(), AppDetailToolbarState().visibleActions)
  }

  @Test
  fun baseStateOnlyShowsSortAction() {
    val state = AppDetailToolbarState(baseActionsReady = true)

    assertEquals(listOf(AppDetailToolbarAction.SORT), state.visibleActions)
  }

  @Test
  fun allActionsUseStableDisplayOrder() {
    val state = AppDetailToolbarState(
      baseActionsReady = true,
      toolbarCollapsed = true,
      harmonyToggleVisible = true,
      processVisible = true,
      compareVisible = true,
      processLabel = "Close process mode"
    )

    assertEquals(
      listOf(
        AppDetailToolbarAction.SORT,
        AppDetailToolbarAction.HARMONY_TOGGLE,
        AppDetailToolbarAction.PROCESS,
        AppDetailToolbarAction.COMPARE,
        AppDetailToolbarAction.QUICK_LAUNCH
      ),
      state.visibleActions
    )
  }

  @Test
  fun asynchronousActionsRemainVisibleBeforeBaseSetup() {
    val state = AppDetailToolbarState(
      toolbarCollapsed = true,
      processVisible = true
    )

    assertEquals(
      listOf(
        AppDetailToolbarAction.PROCESS,
        AppDetailToolbarAction.QUICK_LAUNCH
      ),
      state.visibleActions
    )
  }
}
