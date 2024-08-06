package com.absinthe.libchecker.api.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAppUpdateInfo(
  val app: App
) {
  @JsonClass(generateAdapter = true)
  data class App(
    val version: String,
    val versionCode: Int,
    val extra: Extra,
    val link: String,
    val note: String?
  ) {
    @JsonClass(generateAdapter = true)
    data class Extra(
      val target: Int,
      val min: Int,
      val compile: Int,
      val packageSize: Int
    )
  }
}
