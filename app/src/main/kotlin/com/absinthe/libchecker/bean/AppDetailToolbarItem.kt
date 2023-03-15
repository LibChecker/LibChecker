package com.absinthe.libchecker.bean

data class AppDetailToolbarItem(
  val icon: Int,
  val tooltipTextRes: Int,
  val onClick: () -> Unit
)
