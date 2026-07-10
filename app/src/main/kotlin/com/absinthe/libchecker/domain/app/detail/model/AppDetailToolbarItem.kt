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
