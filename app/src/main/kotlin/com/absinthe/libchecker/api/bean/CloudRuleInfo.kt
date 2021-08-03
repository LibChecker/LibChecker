package com.absinthe.libchecker.api.bean

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudRuleInfo(
  @field:Json(name = "version") val version: Int,
  @field:Json(name = "count") val count: Int
)
