package com.absinthe.libchecker.features.applist.detail.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KotlinToolingMetadata(
  val buildSystem: String,
  val buildSystemVersion: String,
  val buildPlugin: String,
  val buildPluginVersion: String,
  val projectTargets: Array<ProjectTarget>?
) {
  @JsonClass(generateAdapter = true)
  data class ProjectTarget(
    val target: String,
    val platformType: String,
    val extras: Extras?
  )

  @JsonClass(generateAdapter = true)
  data class Extras(
    val android: AndroidExtras?
  )

  @JsonClass(generateAdapter = true)
  data class AndroidExtras(
    val sourceCompatibility: String?,
    val targetCompatibility: String?
  )
}
