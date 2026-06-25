package com.absinthe.libchecker.domain.snapshot.sync

import com.absinthe.libchecker.domain.app.PackageChangeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber

class SnapshotPackageChangeProcessor(
  private val processPackageChange: suspend (PackageChangeState) -> Unit
) {
  private val pendingPackageChanges = Channel<PackageChangeState>(Channel.UNLIMITED)
  private var job: Job? = null

  fun enqueue(
    scope: CoroutineScope,
    packageChangeState: PackageChangeState
  ) {
    val result = pendingPackageChanges.trySend(packageChangeState)
    if (result.isFailure) {
      Timber.w(result.exceptionOrNull(), "Failed to enqueue snapshot package change")
    }
    startIfNeeded(scope)
  }

  fun cancel() {
    job?.cancel()
    job = null
  }

  private fun startIfNeeded(scope: CoroutineScope) {
    if (job?.isActive == true) {
      return
    }
    job = scope.launch(Dispatchers.IO) {
      for (packageChangeState in pendingPackageChanges) {
        Timber.d("Process snapshot package change: $packageChangeState")
        processPackageChange(packageChangeState)
      }
    }
  }
}
