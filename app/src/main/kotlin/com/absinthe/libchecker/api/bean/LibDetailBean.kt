package com.absinthe.libchecker.api.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LibDetailBean(
  val data: List<Data>,
  val uuid: String
) {
  @JsonClass(generateAdapter = true)
  data class Data(
    val locale: String,
    val data: DataBean
  )

  @JsonClass(generateAdapter = true)
  data class DataBean(
    val label: String,
    val dev_team: String,
    val rule_contributors: List<String>,
    val description: String,
    val source_link: String
  )
}
