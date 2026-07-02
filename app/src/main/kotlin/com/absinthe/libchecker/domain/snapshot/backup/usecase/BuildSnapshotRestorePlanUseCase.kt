package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotRestorePlanUseCase(
  private val contentResolver: ContentResolver
) {

  suspend operator fun invoke(uri: Uri): SnapshotRestorePlan = withContext(Dispatchers.IO) {
    if (
      uri.toString().endsWith(DATABASE_BACKUP_EXTENSION, ignoreCase = true) ||
      uri.hasSqliteHeader()
    ) {
      SnapshotRestorePlan.DatabaseBackup
    } else {
      SnapshotRestorePlan.ArchiveBackup
    }
  }

  fun shouldRestoreFromLaunchUri(uri: Uri): Boolean {
    return uri.scheme == ContentResolver.SCHEME_CONTENT &&
      (
        uri.path?.endsWith(SNAPSHOT_ARCHIVE_EXTENSION, ignoreCase = true) == true ||
          uri.path?.endsWith(DATABASE_BACKUP_EXTENSION, ignoreCase = true) == true
        )
  }

  private fun Uri.hasSqliteHeader(): Boolean {
    return runCatching {
      val expectedHeader = SQLITE_HEADER.toByteArray(Charsets.US_ASCII)
      val actualHeader = ByteArray(expectedHeader.size)
      contentResolver.openInputStream(this)?.use { input ->
        input.read(actualHeader) == expectedHeader.size &&
          actualHeader.contentEquals(expectedHeader)
      } == true
    }.getOrDefault(false)
  }

  private companion object {
    private const val DATABASE_BACKUP_EXTENSION = ".sqlite3"
    private const val SNAPSHOT_ARCHIVE_EXTENSION = ".lcss"
    private const val SQLITE_HEADER = "SQLite format 3\u0000"
  }
}

sealed interface SnapshotRestorePlan {
  data object DatabaseBackup : SnapshotRestorePlan
  data object ArchiveBackup : SnapshotRestorePlan
}
