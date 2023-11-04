package com.absinthe.libchecker.features.applist.detail.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KotlinToolingMetadata(
  val buildPlugin: String,
  val buildPluginVersion: String
)
