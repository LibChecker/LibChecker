package com.absinthe.libchecker.domain.app

import android.content.ContentResolver
import android.net.Uri

class ExportAppPackageShareFileUseCase(
  private val contentResolver: ContentResolver
) {

  operator fun invoke(shareFile: AppPackageShareFile, destinationUri: Uri) {
    contentResolver.openOutputStream(destinationUri)?.use { output ->
      shareFile.file.inputStream().use { input ->
        input.copyTo(output)
      }
    } ?: error("OutputStream is null")
  }
}
