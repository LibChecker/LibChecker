package com.absinthe.libchecker.domain.app.model

data class AppInstallSource(
  val initiatingPackageName: String?,
  val originatingPackageName: String?,
  val installingPackageName: String?
)
