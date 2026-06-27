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
import com.absinthe.libchecker.domain.snapshot.SnapshotDatabaseBackupRestoreResult
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

  fun getRestorePlan(uri: Uri): SnapshotRestorePlan = buildSnapshotRestorePlanUseCase(uri)

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

  fun restoreDatabaseBackup(
    roomBackup: RoomBackup,
    uri: Uri,
    cacheDir: File,
    resultAction: (SnapshotDatabaseBackupRestoreResult?) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val result = runCatching {
        restoreSnapshotDatabaseBackupUseCaseFactory(roomBackup)(uri, cacheDir)
      }.onSuccess { result ->
        result?.let {
          Timber.d(
            "success: ${it.success}, message: ${it.message}, exitCode: ${it.exitCode}"
          )
        }
      }.onFailure {
        Timber.e(it)
      }.getOrNull()

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

  fun restore(
    uri: Uri,
    resultAction: (SnapshotArchiveUseCase.RestoreResult?) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        restoreSnapshotArchiveFromUriUseCase(uri)
      }.onFailure {
        Timber.e("restore with new format failed: $it")
        withContext(Dispatchers.Main) {
          resultAction(null)
        }
        return@launch
      }.onSuccess { result ->
        if (result == null) {
          withContext(Dispatchers.Main) {
            resultAction(null)
          }
          return@launch
        }
        result.latestTimeStamp?.let(snapshotSelectionUseCase::setCurrentTimestamp)
        withContext(Dispatchers.Main) {
          resultAction(result)
        }
      }
    }
  }

  fun getFormatDateString(timestamp: Long): String {
    return formatSnapshotTimestampUseCase(timestamp)
  }
}
