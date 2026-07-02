package com.absinthe.libchecker.data.snapshot

import androidx.room3.RoomDatabase
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestoreResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestorer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class LocalSnapshotDatabaseBackupRestorer(
  private val roomBackup: RoomBackup,
  private val databaseName: String,
  private val database: () -> RoomDatabase
) : SnapshotDatabaseBackupRestorer {

  override suspend fun restore(backupFile: File): SnapshotDatabaseBackupRestoreResult {
    return suspendCancellableCoroutine { continuation ->
      var completed = false

      fun complete(result: SnapshotDatabaseBackupRestoreResult) {
        completed = true
        if (continuation.isActive) {
          continuation.resume(result)
        }
      }

      try {
        roomBackup
          .database(database(), databaseName)
          .enableLogDebug(true)
          .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
          .backupLocationCustomFile(backupFile)
          .onCompleteListener { success, message, exitCode ->
            complete(
              SnapshotDatabaseBackupRestoreResult(
                success = success,
                message = message,
                exitCode = exitCode
              )
            )
          }
          .restore()

        if (!completed) {
          complete(
            SnapshotDatabaseBackupRestoreResult(
              success = false,
              message = "restore did not complete",
              exitCode = -1
            )
          )
        }
      } catch (t: Throwable) {
        if (continuation.isActive) {
          continuation.resumeWithException(t)
        }
      }
    }
  }
}
