package com.absinthe.libchecker.features.applist.detail.bean

data class AppDetailToolbarItem(
  val icon: Int,
  var tooltipTextRes: Int,
  val onClick: () -> Unit
)
