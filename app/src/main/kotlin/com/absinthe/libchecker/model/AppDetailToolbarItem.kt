package com.absinthe.libchecker.model

data class AppDetailToolbarItem(
  val icon: Int,
  var tooltipTextRes: Int,
  val onClick: () -> Unit
)
