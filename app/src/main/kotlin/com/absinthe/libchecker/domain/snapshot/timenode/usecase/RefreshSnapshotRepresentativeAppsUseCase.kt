package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotRepresentativeApps

class RefreshSnapshotRepresentativeAppsUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val selectRepresentativeApps: SelectSnapshotRepresentativeAppsUseCase
) {

  suspend fun update(
    timestamp: Long,
    previousTimestamp: Long?
  ) {
    val timestampItem = snapshotRepository.getTimeStamp(timestamp) ?: return
    val currentItems = snapshotRepository.getSnapshotSummaries(timestamp)
    val previousItems: List<SnapshotItem> =
      if (previousTimestamp != null && previousTimestamp > 0L && previousTimestamp != timestamp) {
        snapshotRepository.getSnapshotSummaries(previousTimestamp)
      } else {
        emptyList()
      }
    val trackPackageNames = getTrackPackageNames()
    snapshotRepository.updateTimeStamp(
      timestampItem.copy(
        topApps = SnapshotRepresentativeApps.encode(
          selectRepresentativeApps(
            previousItems = previousItems,
            currentItems = currentItems,
            trackPackageNames = trackPackageNames
          )
        )
      )
    )
  }

  suspend operator fun invoke(timeStamps: List<TimeStampItem>): List<TimeStampItem> {
    if (timeStamps.none { SnapshotRepresentativeApps.needsRefresh(it.topApps) }) {
      return timeStamps
    }

    val sortedTimeStamps = timeStamps.sortedBy(TimeStampItem::timestamp)
    val refreshIndexes = sortedTimeStamps.indices.filter {
      SnapshotRepresentativeApps.needsRefresh(sortedTimeStamps[it].topApps)
    }
    val requiredIndexes = buildSet {
      for (index in refreshIndexes) {
        if (index > 0) add(index - 1)
        add(index)
      }
    }.filter(sortedTimeStamps.indices::contains)
    val snapshotsByTimestamp = requiredIndexes.associateBy(
      keySelector = { sortedTimeStamps[it].timestamp },
      valueTransform = { snapshotRepository.getSnapshotSummaries(sortedTimeStamps[it].timestamp) }
    )
    val trackPackageNames = getTrackPackageNames()
    val updatedTimeStamps = sortedTimeStamps.mapIndexed { index, item ->
      if (!SnapshotRepresentativeApps.needsRefresh(item.topApps)) {
        item
      } else {
        val previousItems = sortedTimeStamps.getOrNull(index - 1)
          ?.timestamp
          ?.let(snapshotsByTimestamp::get)
          .orEmpty()
        val currentItems = snapshotsByTimestamp[item.timestamp].orEmpty()
        val updatedItem = item.copy(
          topApps = SnapshotRepresentativeApps.encode(
            selectRepresentativeApps(
              previousItems = previousItems,
              currentItems = currentItems,
              trackPackageNames = trackPackageNames
            )
          )
        )
        snapshotRepository.updateTimeStamp(updatedItem)
        updatedItem
      }
    }.associateBy(TimeStampItem::timestamp)
    return timeStamps.map { updatedTimeStamps.getValue(it.timestamp) }
  }

  private suspend fun getTrackPackageNames(): Set<String> {
    return snapshotRepository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
  }
}
