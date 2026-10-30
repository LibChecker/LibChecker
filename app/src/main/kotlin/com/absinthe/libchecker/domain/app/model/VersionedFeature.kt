package com.absinthe.libchecker.domain.app.model

data class VersionedFeature(
  val featureType: Int,
  val version: String? = null,
  val extras: Map<String, String?>? = null,
  val appFunctions: List<AppFunctionInfo> = emptyList()
)
