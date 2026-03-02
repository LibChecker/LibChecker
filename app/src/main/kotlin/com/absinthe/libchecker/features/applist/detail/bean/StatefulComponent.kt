package com.absinthe.libchecker.features.applist.detail.bean

data class StatefulComponent(
  val componentName: String,
  val enabled: Boolean = true,
  val exported: Boolean = false,
  val processName: String
)
