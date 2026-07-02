package com.absinthe.libchecker.domain.snapshot.backup.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.backup.model.SnapshotBackupTarget
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExportResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestoreResult
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BackupSnapshotArchiveToUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BuildSnapshotRestorePlanUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.CreateSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.GetSnapshotBackupTargetUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotArchiveFromUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotArchiveBackupResult
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotRestorePlan
import com.absinthe.libchecker.domain.snapshot.display.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection
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
  private val snapshotSelection: SnapshotSelection
) : ViewModel() {

  suspend fun onLocalBackupRequested(isExternalStorageWritable: Boolean): LocalBackupAction = withContext(Dispatchers.IO) {
    if (!isExternalStorageWritable) {
      return@withContext LocalBackupAction.StorageUnavailable
    }

    when (val target = getSnapshotBackupTargetUseCase()) {
      is SnapshotBackupTarget.Archive -> LocalBackupAction.CreateArchive(target.fileName)
      SnapshotBackupTarget.Database -> LocalBackupAction.CreateDatabase
      SnapshotBackupTarget.Empty -> LocalBackupAction.NoSnapshot
    }
  }

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
  ): SnapshotDatabaseBackupRestoreResult? {
    return runCatching {
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
          val result = restoreDatabaseBackup(roomBackup, uri, cacheDir)
          RestoreBackupResult.DatabaseBackup(success = result?.success == true)
        }

        SnapshotRestorePlan.ArchiveBackup -> RestoreBackupResult.ArchiveBackup(restoreArchive(uri))
      }

      withContext(Dispatchers.Main) {
        resultAction(result)
      }
    }
  }

  fun backup(uri: Uri, resultAction: (SnapshotArchiveBackupResult) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    val result = runCatching {
      backupSnapshotArchiveToUriUseCase(uri)
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(SnapshotArchiveBackupResult.Failed)
    withContext(Dispatchers.Main) {
      resultAction(result)
    }
  }

  private suspend fun restoreArchive(uri: Uri): ArchiveRestoreSummary? {
    val result = runCatching {
      restoreSnapshotArchiveFromUriUseCase(uri)
    }.onFailure {
      Timber.e("restore with new format failed: $it")
    }.getOrNull() ?: return null

    result.latestTimeStamp?.let(snapshotSelection::setCurrentTimestamp)
    return result.toArchiveRestoreSummary()
  }

  private fun SnapshotArchiveUseCase.RestoreResult.toArchiveRestoreSummary(): ArchiveRestoreSummary {
    return ArchiveRestoreSummary(
      items = timeStampCounts.map { (timestamp, count) ->
        ArchiveRestoreSummaryItem(
          formattedTimestamp = formatSnapshotTimestampUseCase(timestamp),
          count = count
        )
      }
    )
  }

  private suspend fun getRestorePlan(uri: Uri): SnapshotRestorePlan = buildSnapshotRestorePlanUseCase(uri)

  sealed interface RestoreBackupResult {
    data class DatabaseBackup(val success: Boolean) : RestoreBackupResult

    data class ArchiveBackup(
      val summary: ArchiveRestoreSummary?
    ) : RestoreBackupResult
  }

  data class ArchiveRestoreSummary(
    val items: List<ArchiveRestoreSummaryItem>
  )

  data class ArchiveRestoreSummaryItem(
    val formattedTimestamp: String,
    val count: Int
  )

  sealed interface LocalBackupAction {
    data class CreateArchive(val fileName: String) : LocalBackupAction
    data object CreateDatabase : LocalBackupAction
    data object StorageUnavailable : LocalBackupAction
    data object NoSnapshot : LocalBackupAction
  }
}
