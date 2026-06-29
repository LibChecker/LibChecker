package com.absinthe.libchecker.domain.app.detail.model

data class AppPropItem(
  val key: String,
  val value: String,
  val displayValue: String,
  val resourceId: Int?,
  val resourceType: String?
)
