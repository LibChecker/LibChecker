package com.absinthe.libchecker.domain.snapshot

import android.content.ContentResolver
import android.net.Uri

class BackupSnapshotArchiveToUriUseCase(
  private val contentResolver: ContentResolver,
  private val snapshotArchive: SnapshotArchiveUseCase
) {

  suspend operator fun invoke(uri: Uri) {
    contentResolver.openOutputStream(uri)?.let { outputStream ->
      snapshotArchive.backup(outputStream)
    }
  }
}
