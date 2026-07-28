package com.absinthe.libchecker.domain.app.detail.header

data class AppDetailHeaderTitleData(
  val packageName: String,
  val appName: String?,
  val title: String,
  val versionInfo: String,
  val isAppInfoAvailable: Boolean
)
