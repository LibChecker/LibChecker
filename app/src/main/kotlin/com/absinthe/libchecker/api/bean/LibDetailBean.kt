package com.absinthe.libchecker.api.bean

import com.squareup.moshi.Json

data class LibDetailBean(
  @field:Json(name = "label") val label: String,
  @field:Json(name = "team") val team: String,
  @field:Json(name = "iconUrl") val iconUrl: String,
  @field:Json(name = "contributors") val contributors: List<String>,
  @field:Json(name = "description") val description: String,
  @field:Json(name = "relativeUrl") val relativeUrl: String
)
