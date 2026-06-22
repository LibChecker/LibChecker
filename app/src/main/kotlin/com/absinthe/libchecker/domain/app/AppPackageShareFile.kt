package com.absinthe.libchecker.domain.app

import java.io.File

data class AppPackageShareFile(
  val file: File,
  val mimeType: String
)
