package com.absinthe.libchecker.domain.snapshot.display

import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository

class SnapshotDashboardCounter(
  private val snapshotRepository: SnapshotRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(timestamp: Long): SnapshotDashboardCount {
    return SnapshotDashboardCount(
      snapshotCount = snapshotRepository.getSnapshots(timestamp).size,
      appCount = installedAppRepository.getApplicationCount()
    )
  }
}

data class SnapshotDashboardCount(
  val snapshotCount: Int,
  val appCount: Int
)
