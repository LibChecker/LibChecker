package com.absinthe.libchecker.domain.snapshot

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import okio.buffer
import okio.sink
import okio.source

class PrepareRoomBackupRestoreFileUseCase(
  private val contentResolver: ContentResolver
) {

  suspend operator fun invoke(
    uri: Uri,
    restoreFile: File
  ): File? {
    return contentResolver.openInputStream(uri)?.let { inputStream ->
      inputStream.source().buffer().use { source ->
        restoreFile.outputStream().sink().buffer().use { sink ->
          source.readAll(sink)
        }
      }
      restoreFile
    }
  }
}
