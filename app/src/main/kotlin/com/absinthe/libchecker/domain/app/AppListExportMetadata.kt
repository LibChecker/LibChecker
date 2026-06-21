package com.absinthe.libchecker.domain.app

interface AppListExportMetadata {
  val versionName: String
  val versionCode: Long

  fun formatAbi(abi: Short): String
}
