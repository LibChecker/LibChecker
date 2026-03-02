package com.absinthe.libchecker.features.applist.detail

data class VersionedFeature(
  val featureType: Int,
  val version: String? = null,
  val extras: Map<String, String?>? = null
)
