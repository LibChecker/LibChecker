package com.absinthe.libchecker.domain.statistics.chart.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatisticRemoteManifest(
  val schemaVersion: Int,
  val bundleVersion: Int,
  val bundleSha256: String,
  val bundleSize: Long,
  val minimumAppVersionCode: Long
)
