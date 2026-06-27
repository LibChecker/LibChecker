package com.absinthe.libchecker.features.album.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.BackupSnapshotArchiveToUriUseCase
import com.absinthe.libchecker.domain.snapshot.CreateSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotBackupTargetUseCase
import com.absinthe.libchecker.domain.snapshot.RestoreSnapshotArchiveFromUriUseCase
import com.absinthe.libchecker.domain.snapshot.RestoreSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotBackupTarget
import com.absinthe.libchecker.domain.snapshot.SnapshotDatabaseBackupExportResult
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase
import com.absinthe.libchecker.domain.snapshot.backup.BuildSnapshotRestorePlanUseCase
import com.absinthe.libchecker.domain.snapshot.backup.SnapshotRestorePlan
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SnapshotBackupViewModel(
  private val backupSnapshotArchiveToUriUseCase: BackupSnapshotArchiveToUriUseCase,
  private val restoreSnapshotArchiveFromUriUseCase: RestoreSnapshotArchiveFromUriUseCase,
  private val getSnapshotBackupTargetUseCase: GetSnapshotBackupTargetUseCase,
  private val buildSnapshotRestorePlanUseCase: BuildSnapshotRestorePlanUseCase,
  private val createSnapshotDatabaseBackupUseCaseFactory: (RoomBackup) -> CreateSnapshotDatabaseBackupUseCase,
  private val restoreSnapshotDatabaseBackupUseCaseFactory: (RoomBackup) -> RestoreSnapshotDatabaseBackupUseCase,
  private val formatSnapshotTimestampUseCase: FormatSnapshotTimestampUseCase,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase
) : ViewModel() {

  fun getBackupTarget(): SnapshotBackupTarget = getSnapshotBackupTargetUseCase()

  fun shouldRestoreFromLaunchUri(uri: Uri): Boolean {
    return buildSnapshotRestorePlanUseCase.shouldRestoreFromLaunchUri(uri)
  }

  fun createDatabaseBackup(
    roomBackup: RoomBackup,
    onComplete: (SnapshotDatabaseBackupExportResult) -> Unit
  ): Result<Unit> {
    return runCatching {
      createSnapshotDatabaseBackupUseCaseFactory(roomBackup)(onComplete)
    }.onFailure {
      Timber.e(it)
    }
  }

  private suspend fun restoreDatabaseBackup(
    roomBackup: RoomBackup,
    uri: Uri,
    cacheDir: File
  ) {
    runCatching {
      restoreSnapshotDatabaseBackupUseCaseFactory(roomBackup)(uri, cacheDir)
    }.onSuccess { result ->
      result?.let {
        Timber.d(
          "success: ${it.success}, message: ${it.message}, exitCode: ${it.exitCode}"
        )
      }
    }.onFailure {
      Timber.e(it)
    }
  }

  fun restoreBackup(
    roomBackup: RoomBackup,
    uri: Uri,
    cacheDir: File,
    resultAction: (RestoreBackupResult) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val result = when (getRestorePlan(uri)) {
        SnapshotRestorePlan.DatabaseBackup -> {
          restoreDatabaseBackup(roomBackup, uri, cacheDir)
          RestoreBackupResult.DatabaseBackup
        }

        SnapshotRestorePlan.ArchiveBackup -> RestoreBackupResult.ArchiveBackup(
          restoreArchive(uri)
        )
      }

      withContext(Dispatchers.Main) {
        resultAction(result)
      }
    }
  }

  fun backup(uri: Uri, resultAction: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    runCatching {
      backupSnapshotArchiveToUriUseCase(uri)
    }.onFailure {
      Timber.e(it)
    }
    withContext(Dispatchers.Main) {
      resultAction()
    }
  }

  private suspend fun restoreArchive(uri: Uri): SnapshotArchiveUseCase.RestoreResult? {
    val result = runCatching {
      restoreSnapshotArchiveFromUriUseCase(uri)
    }.onFailure {
      Timber.e("restore with new format failed: $it")
    }.getOrNull() ?: return null

    result.latestTimeStamp?.let(snapshotSelectionUseCase::setCurrentTimestamp)
    return result
  }

  fun getFormatDateString(timestamp: Long): String {
    return formatSnapshotTimestampUseCase(timestamp)
  }

  private fun getRestorePlan(uri: Uri): SnapshotRestorePlan = buildSnapshotRestorePlanUseCase(uri)

  sealed interface RestoreBackupResult {
    data object DatabaseBackup : RestoreBackupResult

    data class ArchiveBackup(
      val result: SnapshotArchiveUseCase.RestoreResult?
    ) : RestoreBackupResult
  }
}
