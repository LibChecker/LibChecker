package com.absinthe.libchecker.domain.app.model

data class AppFunctionInfo(
  val id: String,
  val name: String = id.substringAfterLast('#'),
  val description: String? = null
)
