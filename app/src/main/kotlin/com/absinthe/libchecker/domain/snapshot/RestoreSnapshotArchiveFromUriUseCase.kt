package com.absinthe.libchecker.domain.snapshot

import android.content.ContentResolver
import android.net.Uri

class RestoreSnapshotArchiveFromUriUseCase(
  private val contentResolver: ContentResolver,
  private val snapshotArchive: SnapshotArchiveUseCase
) {

  suspend operator fun invoke(uri: Uri): SnapshotArchiveUseCase.RestoreResult? {
    return contentResolver.openInputStream(uri)?.let { inputStream ->
      snapshotArchive.restore(inputStream)
    }
  }
}
