package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.content.ContentResolver
import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase

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
