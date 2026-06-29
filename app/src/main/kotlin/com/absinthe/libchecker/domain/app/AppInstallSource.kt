package com.absinthe.libchecker.domain.app

data class AppInstallSource(
  val initiatingPackageName: String?,
  val originatingPackageName: String?,
  val installingPackageName: String?
)
