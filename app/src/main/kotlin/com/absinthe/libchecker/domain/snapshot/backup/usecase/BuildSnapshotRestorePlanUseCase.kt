package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.content.ContentResolver
import android.net.Uri

class BuildSnapshotRestorePlanUseCase {

  operator fun invoke(uri: Uri): SnapshotRestorePlan {
    return if (uri.toString().endsWith(DATABASE_BACKUP_EXTENSION)) {
      SnapshotRestorePlan.DatabaseBackup
    } else {
      SnapshotRestorePlan.ArchiveBackup
    }
  }

  fun shouldRestoreFromLaunchUri(uri: Uri): Boolean {
    return uri.scheme == ContentResolver.SCHEME_CONTENT &&
      uri.path?.endsWith(SNAPSHOT_ARCHIVE_EXTENSION) == true
  }

  private companion object {
    private const val DATABASE_BACKUP_EXTENSION = ".sqlite3"
    private const val SNAPSHOT_ARCHIVE_EXTENSION = ".lcss"
  }
}

sealed interface SnapshotRestorePlan {
  data object DatabaseBackup : SnapshotRestorePlan
  data object ArchiveBackup : SnapshotRestorePlan
}
