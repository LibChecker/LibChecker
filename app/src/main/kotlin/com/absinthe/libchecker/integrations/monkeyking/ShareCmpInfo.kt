package com.absinthe.libchecker.integrations.monkeyking

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShareCmpInfo(
  val pkg: String,
  val components: List<Component>,
) {
  @JsonClass(generateAdapter = true)
  data class Component(
    val type: String,
    val name: String,
    val block: Boolean,
  )
}
