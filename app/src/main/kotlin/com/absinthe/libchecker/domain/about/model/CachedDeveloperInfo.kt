package com.absinthe.libchecker.domain.about.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CachedDeveloperInfo(
  val name: String,
  val desc: String,
  val github: String,
  val avatarUrl: String
)
