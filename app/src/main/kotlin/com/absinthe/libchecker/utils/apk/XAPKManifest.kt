package com.absinthe.libchecker.utils.apk

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XAPKManifest(
  val xapk_version: Int,
  val package_name: String,
  val name: String,
  val version_code: Int,
  val version_name: String,
  val split_configs: List<String>,
  val split_apks: List<SplitConfig>
) {
  @JsonClass(generateAdapter = true)
  data class SplitConfig(
    val file: String,
    val id: String
  )
}
