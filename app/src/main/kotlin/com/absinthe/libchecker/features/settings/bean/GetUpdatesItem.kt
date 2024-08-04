package com.absinthe.libchecker.features.settings.bean

data class GetUpdatesItem(
  val text: String,
  val iconRes: Int,
  val action: () -> Unit
)
