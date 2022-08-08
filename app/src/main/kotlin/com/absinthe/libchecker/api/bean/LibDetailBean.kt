package com.absinthe.libchecker.api.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LibDetailBean(
  val label: String,
  val team: String,
  val iconUrl: String,
  val contributors: List<String>,
  val description: String,
  val relativeUrl: String
)
