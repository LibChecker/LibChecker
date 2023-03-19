package com.absinthe.libchecker.model

data class AppDetailToolbarItem(
  val icon: Int,
  val tooltipTextRes: Int,
  val onClick: () -> Unit
)
