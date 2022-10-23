package com.absinthe.libchecker.api.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudRuleInfo(
  val version: Int,
  val count: Int
)
