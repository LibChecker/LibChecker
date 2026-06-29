package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.utils.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotTimeNodeListDataUseCase(
  private val getSnapshotPackageIconSources: GetSnapshotPackageIconSourcesUseCase
) {

  suspend operator fun invoke(timeStamps: List<TimeStampItem>): SnapshotTimeNodeListData = withContext(Dispatchers.Default) {
    val items = timeStamps.map { item ->
      SnapshotTimeNodeItem(
        timestamp = item.timestamp,
        topAppPackageNames = item.topApps.toPackageNames()
      )
    }
    val topAppPackageNames = items.asSequence()
      .flatMap { it.topAppPackageNames.asSequence() }
      .distinct()
      .toList()
    SnapshotTimeNodeListData(
      items = items,
      packageIconSources = getSnapshotPackageIconSources(topAppPackageNames)
    )
  }

  private fun String?.toPackageNames(): List<String> {
    return this?.fromJson<List<String>>(List::class.java, String::class.java).orEmpty()
  }
}
