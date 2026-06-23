package com.absinthe.libchecker.features.applist.detail.bean

data class AppPropItem(
  val key: String,
  val value: String,
  val displayValue: String,
  val resourceId: Int?,
  val resourceType: String?
)
