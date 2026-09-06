package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.annotation.STATIC
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDetailToolbarStateTest {

  @Test
  fun advancedMenuFollowsProcessOnlyOnComponentTabs() {
    val base = AppDetailToolbarState(baseActionsReady = true, onlineRuleAnalysisVisible = true)
    for (type in listOf(ACTIVITY, SERVICE, RECEIVER, PROVIDER)) {
      assertEquals(
        listOf(AppDetailToolbarAction.SORT, AppDetailToolbarAction.ONLINE_RULE_ANALYSIS, AppDetailToolbarAction.ADVANCED_MENU),
        base.copy(selectedType = type).visibleActions
      )
      assertEquals(
        listOf(
          AppDetailToolbarAction.SORT,
          AppDetailToolbarAction.ONLINE_RULE_ANALYSIS,
          AppDetailToolbarAction.PROCESS,
          AppDetailToolbarAction.ADVANCED_MENU
        ),
        base.copy(selectedType = type, processVisible = true).visibleActions
      )
    }
    for (type in listOf(NATIVE, STATIC, PERMISSION, METADATA, SIGNATURES, DEX, ALL)) {
      assertEquals(base.visibleActions, base.copy(selectedType = type).visibleActions)
    }
    assertEquals(emptyList<AppDetailToolbarAction>(), AppDetailToolbarState(selectedType = ACTIVITY).visibleActions)
  }

  @Test
  fun defaultStateHasNoVisibleActions() {
    assertEquals(emptyList<AppDetailToolbarAction>(), AppDetailToolbarState().visibleActions)
  }

  @Test
  fun baseStateHidesOnlineRuleAnalysisWithoutSelectedRules() {
    val state = AppDetailToolbarState(baseActionsReady = true)

    assertEquals(
      listOf(AppDetailToolbarAction.SORT),
      state.visibleActions
    )
  }

  @Test
  fun baseStateShowsOnlineRuleAnalysisWithSelectedRules() {
    val state = AppDetailToolbarState(
      baseActionsReady = true,
      onlineRuleAnalysisVisible = true
    )

    assertEquals(
      listOf(
        AppDetailToolbarAction.SORT,
        AppDetailToolbarAction.ONLINE_RULE_ANALYSIS
      ),
      state.visibleActions
    )
  }

  @Test
  fun allActionsUseStableDisplayOrder() {
    val state = AppDetailToolbarState(
      baseActionsReady = true,
      toolbarCollapsed = true,
      onlineRuleAnalysisVisible = true,
      harmonyToggleVisible = true,
      processVisible = true,
      compareVisible = true,
      processLabel = "Close process mode"
    )

    assertEquals(
      listOf(
        AppDetailToolbarAction.SORT,
        AppDetailToolbarAction.ONLINE_RULE_ANALYSIS,
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
