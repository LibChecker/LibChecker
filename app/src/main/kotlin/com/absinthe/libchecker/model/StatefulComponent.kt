package com.absinthe.libchecker.model

data class StatefulComponent(
  val componentName: String,
  val enabled: Boolean = true,
  val exported: Boolean = false,
  val processName: String
)
