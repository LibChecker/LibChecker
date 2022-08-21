package com.absinthe.libchecker.bean

data class AdvancedMenuItem(
  val labelRes: Int,
  var isSelected: Boolean,
  val action: () -> Unit
)
