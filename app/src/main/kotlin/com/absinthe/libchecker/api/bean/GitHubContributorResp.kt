package com.absinthe.libchecker.api.bean

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubContributorResp(
  val login: String? = null,
  val name: String? = null,
  val email: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "html_url") val htmlUrl: String? = null,
  val type: String? = null,
  val contributions: Int
)
