package com.absinthe.libchecker.domain.app.detail.model

data class StatefulComponent(
  val componentName: String,
  val enabled: Boolean = true,
  val exported: Boolean = false,
  val processName: String
)
