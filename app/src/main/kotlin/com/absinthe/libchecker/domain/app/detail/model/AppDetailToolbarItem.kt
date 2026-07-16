package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes

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
  QUICK_LAUNCH,
  PROCESS,
  HARMONY_TOGGLE,
  COMPARE
}

data class AppDetailToolbarState(
  val baseActionsReady: Boolean = false,
  val toolbarCollapsed: Boolean = false,
  val harmonyToggleVisible: Boolean = false,
  val processVisible: Boolean = false,
  val compareVisible: Boolean = false,
  val processLabel: String = ""
) {
  val visibleActions: List<AppDetailToolbarAction>
    get() = buildList {
      if (baseActionsReady) {
        add(AppDetailToolbarAction.SORT)
        if (harmonyToggleVisible) {
          add(AppDetailToolbarAction.HARMONY_TOGGLE)
        }
      }
      if (processVisible) {
        add(AppDetailToolbarAction.PROCESS)
      }
      if (compareVisible) {
        add(AppDetailToolbarAction.COMPARE)
      }
      if (toolbarCollapsed) {
        add(AppDetailToolbarAction.QUICK_LAUNCH)
      }
    }
}
