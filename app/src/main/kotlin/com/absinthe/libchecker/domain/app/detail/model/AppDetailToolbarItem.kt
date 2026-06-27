package com.absinthe.libchecker.domain.app.detail.model

data class AppDetailToolbarItem(
  val icon: Int,
  var tooltipTextRes: Int,
  val onClick: () -> Unit
)
