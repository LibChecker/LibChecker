package com.absinthe.libchecker.ui.app

import androidx.annotation.StringRes

data class MenuOptionItem(
  @StringRes val labelRes: Int,
  val option: Int,
  val isChecked: Boolean
) {
  internal constructor(
    @StringRes labelRes: Int,
    option: Int,
    currentOptions: Int
  ) : this(labelRes, option, (currentOptions and option) > 0)
}
