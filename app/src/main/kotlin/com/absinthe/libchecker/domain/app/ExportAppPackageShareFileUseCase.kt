package com.absinthe.libchecker.domain.app

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportAppPackageShareFileUseCase(
  private val contentResolver: ContentResolver
) {

  suspend operator fun invoke(
    shareFile: AppPackageShareFile,
    destinationUri: Uri
  ) = withContext(Dispatchers.IO) {
    contentResolver.openOutputStream(destinationUri)?.use { output ->
      shareFile.file.inputStream().use { input ->
        input.copyTo(output)
      }
    } ?: error("OutputStream is null")
  }
}
