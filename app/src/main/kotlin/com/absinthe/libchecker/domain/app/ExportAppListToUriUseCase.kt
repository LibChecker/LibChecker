package com.absinthe.libchecker.domain.app

import android.content.ContentResolver
import android.net.Uri

class ExportAppListToUriUseCase(
  private val contentResolver: ContentResolver,
  private val exportAppListUseCase: ExportAppListUseCase
) {

  suspend operator fun invoke(
    uri: Uri,
    format: ExportAppListUseCase.Format
  ) {
    contentResolver.openOutputStream(uri)?.use { outputStream ->
      exportAppListUseCase(outputStream, format)
    }
  }
}
