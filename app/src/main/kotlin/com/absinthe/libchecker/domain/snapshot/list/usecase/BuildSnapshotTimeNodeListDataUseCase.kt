package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.display.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotPalette
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotRepresentativeApps
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotTimeNodeListDataUseCase(
  private val getSnapshotPackageIconSources: GetSnapshotPackageIconSourcesUseCase,
  private val getSnapshotCountsByTimestamp: suspend () -> Map<Long, Int> = { emptyMap() },
  private val refreshRepresentativeApps:
  suspend (List<TimeStampItem>) -> List<TimeStampItem> = { it },
  private val formatTimestamp: (Long) -> String = FormatSnapshotTimestampUseCase()::invoke
) {

  suspend operator fun invoke(
    timeStamps: List<TimeStampItem>,
    currentTimestamp: Long? = null
  ): SnapshotTimeNodeListData = withContext(Dispatchers.Default) {
    val refreshedTimeStamps = refreshRepresentativeApps(timeStamps)
    val snapshotCounts = getSnapshotCountsByTimestamp()
    val candidatePackageNamesByTimestamp = refreshedTimeStamps.associateBy(
      keySelector = { it.timestamp },
      valueTransform = { SnapshotRepresentativeApps.decode(it.topApps) }
    )
    val candidatePackageNames = candidatePackageNamesByTimestamp.values
      .asSequence()
      .flatten()
      .distinct()
      .toList()
    val packageIconSources = getSnapshotPackageIconSources(candidatePackageNames)
    val chronologicalIndices = refreshedTimeStamps
      .sortedBy { it.timestamp }
      .mapIndexed { idx, itm -> itm.timestamp to idx }
      .toMap()
    val items = refreshedTimeStamps.map { item ->
      val timestampText = formatTimestamp(item.timestamp)
      val tagColor = chronologicalIndices[item.timestamp]?.let {
        SnapshotPalette.getColor(it)
      }
      SnapshotTimeNodeItem(
        timestamp = item.timestamp,
        timestampText = timestampText,
        description = timestampText,
        topAppPackageNames = candidatePackageNamesByTimestamp[item.timestamp]
          .orEmpty()
          .filter {
            packageIconSources[it] is SnapshotPackageIconSource.InstalledPackage
          }
          .take(VISIBLE_APP_CANDIDATE_LIMIT),
        appCount = snapshotCounts[item.timestamp] ?: 0,
        isCurrent = item.timestamp == currentTimestamp,
        isSelected = item.timestamp == currentTimestamp,
        tagColor = tagColor
      )
    }
    val visiblePackageNames = items.asSequence()
      .flatMap { it.topAppPackageNames.asSequence() }
      .toSet()
    SnapshotTimeNodeListData(
      items = items,
      packageIconSources = packageIconSources.filterKeys(visiblePackageNames::contains)
    )
  }

  private companion object {
    const val VISIBLE_APP_CANDIDATE_LIMIT = 7
  }
}
