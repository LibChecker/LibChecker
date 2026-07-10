package com.absinthe.libchecker.ui.app

import androidx.annotation.StringRes

data class MenuOptionItem(
  @StringRes val labelRes: Int,
  val option: Int,
  val isChecked: Boolean
)
