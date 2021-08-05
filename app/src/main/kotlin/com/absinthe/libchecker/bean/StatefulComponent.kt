package com.absinthe.libchecker.bean

data class StatefulComponent(
  val componentName: String,
  val enabled: Boolean = true,
  val processName: String
)
