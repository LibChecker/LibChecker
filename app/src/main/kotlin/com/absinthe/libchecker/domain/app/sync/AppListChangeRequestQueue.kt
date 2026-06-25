package com.absinthe.libchecker.domain.app.sync

import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import java.util.concurrent.atomic.AtomicInteger

internal class AppListChangeRequestQueue {

  private val pendingChangedPackages = ArrayDeque<PackageChangeState>()
  private val pendingChangedPackagesLock = Any()
  private val generation = AtomicInteger()

  fun start(packageChangeState: PackageChangeState?): ChangeRequest {
    packageChangeState?.let(::addPendingChangedPackage)
    return ChangeRequest(
      forceUpdate = packageChangeState == null,
      generation = generation.incrementAndGet()
    )
  }

  fun isCurrent(request: ChangeRequest): Boolean {
    return generation.get() == request.generation
  }

  fun buildSyncRequest(request: ChangeRequest): SyncAppListChangesUseCase.Request {
    return if (request.forceUpdate) {
      SyncAppListChangesUseCase.Request.RefreshAll
    } else {
      SyncAppListChangesUseCase.Request.ApplyPackageChanges(snapshotPendingChangedPackages())
    }
  }

  fun consumeSyncedRequest(
    request: ChangeRequest,
    syncRequest: SyncAppListChangesUseCase.Request
  ) {
    if (!isCurrent(request)) {
      return
    }

    when (syncRequest) {
      is SyncAppListChangesUseCase.Request.ApplyPackageChanges -> {
        removePendingChangedPackages(syncRequest.changes.size)
      }

      SyncAppListChangesUseCase.Request.RefreshAll -> {
        clearPendingChangedPackages()
      }
    }
  }

  private fun addPendingChangedPackage(packageChangeState: PackageChangeState) {
    synchronized(pendingChangedPackagesLock) {
      pendingChangedPackages.add(packageChangeState)
    }
  }

  private fun snapshotPendingChangedPackages(): List<PackageChangeState> {
    return synchronized(pendingChangedPackagesLock) {
      pendingChangedPackages.toList()
    }
  }

  private fun removePendingChangedPackages(count: Int) {
    synchronized(pendingChangedPackagesLock) {
      repeat(count.coerceAtMost(pendingChangedPackages.size)) {
        pendingChangedPackages.removeFirst()
      }
    }
  }

  private fun clearPendingChangedPackages() {
    synchronized(pendingChangedPackagesLock) {
      pendingChangedPackages.clear()
    }
  }

  class ChangeRequest internal constructor(
    val forceUpdate: Boolean,
    val generation: Int
  )
}
