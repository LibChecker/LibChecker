package com.absinthe.libchecker.domain.app.list.export

interface AppListExportMetadata {
  val versionName: String
  val versionCode: Long

  fun formatAbi(abi: Short): String
}
