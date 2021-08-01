package com.absinthe.libchecker.api.bean

import com.squareup.moshi.Json

data class CloudRuleInfo(
  @field:Json(name = "version") val version: Int,
  @field:Json(name = "count") val count: Int,
  @field:Json(name = "bundles") val bundles: Int
)
