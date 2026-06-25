package com.absinthe.libchecker.domain.app.detail.action

import android.net.Uri
import java.io.File

data class AppPackageShareFile(
  val file: File,
  val mimeType: String,
  val contentUri: Uri
)
