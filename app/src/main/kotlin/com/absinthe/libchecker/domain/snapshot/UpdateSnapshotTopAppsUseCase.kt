package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.toJson

class UpdateSnapshotTopAppsUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(timestamp: Long, items: List<SnapshotDiffItem>) {
    val systemProps = snapshotRepository.getTimeStamp(timestamp)?.systemProps
    val appsList = items.asSequence()
      .map { it.packageName }
      .filter { installedAppRepository.isPackageInstalled(it) }
      .toList()
    snapshotRepository.updateTimeStamp(TimeStampItem(timestamp, appsList.toJson(), systemProps))
  }
}
