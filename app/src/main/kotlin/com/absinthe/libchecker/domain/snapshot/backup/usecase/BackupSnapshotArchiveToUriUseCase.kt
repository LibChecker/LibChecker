package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.content.ContentResolver
import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveUseCase

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
