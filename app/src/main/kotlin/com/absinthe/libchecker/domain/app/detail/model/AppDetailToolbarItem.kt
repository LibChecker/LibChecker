package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.annotation.isComponentType

data class AppDetailToolbarItem(
  val action: AppDetailToolbarAction,
  @DrawableRes val iconRes: Int,
  val label: CharSequence
) {
  val stableId: Long
    get() = action.ordinal.toLong()
}

enum class AppDetailToolbarAction {
  SORT,
  ADVANCED_MENU,
  ONLINE_RULE_ANALYSIS,
  QUICK_LAUNCH,
  PROCESS,
  HARMONY_TOGGLE,
  COMPARE
}

data class AppDetailToolbarState(
  val baseActionsReady: Boolean = false,
  val selectedType: Int? = null,
  val toolbarCollapsed: Boolean = false,
  val onlineRuleAnalysisVisible: Boolean = false,
  val harmonyToggleVisible: Boolean = false,
  val processVisible: Boolean = false,
  val compareVisible: Boolean = false,
  val processLabel: String = ""
) {
  val visibleActions: List<AppDetailToolbarAction>
    get() = buildList {
      if (baseActionsReady) {
        add(AppDetailToolbarAction.SORT)
        if (onlineRuleAnalysisVisible) {
          add(AppDetailToolbarAction.ONLINE_RULE_ANALYSIS)
        }
        if (harmonyToggleVisible) {
          add(AppDetailToolbarAction.HARMONY_TOGGLE)
        }
      }
      if (processVisible) {
        add(AppDetailToolbarAction.PROCESS)
      }
      if (baseActionsReady && selectedType?.let(::isComponentType) == true) {
        add(AppDetailToolbarAction.ADVANCED_MENU)
      }
      if (compareVisible) {
        add(AppDetailToolbarAction.COMPARE)
      }
      if (toolbarCollapsed) {
        add(AppDetailToolbarAction.QUICK_LAUNCH)
      }
    }
}
