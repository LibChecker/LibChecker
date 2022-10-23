package com.absinthe.libchecker.api.bean

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RepoInfoResp(
  @Json(name = "pushed_at") val pushedAt: String
)
