package com.absinthe.libchecker.features.album.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.domain.snapshot.BackupSnapshotArchiveToUriUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotBackupTargetUseCase
import com.absinthe.libchecker.domain.snapshot.RestoreSnapshotArchiveFromUriUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotBackupTarget
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SnapshotBackupViewModel(
  private val backupSnapshotArchiveToUriUseCase: BackupSnapshotArchiveToUriUseCase,
  private val restoreSnapshotArchiveFromUriUseCase: RestoreSnapshotArchiveFromUriUseCase,
  private val getSnapshotBackupTargetUseCase: GetSnapshotBackupTargetUseCase,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase
) : ViewModel() {

  fun getBackupTarget(): SnapshotBackupTarget = getSnapshotBackupTargetUseCase()

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
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }
}
