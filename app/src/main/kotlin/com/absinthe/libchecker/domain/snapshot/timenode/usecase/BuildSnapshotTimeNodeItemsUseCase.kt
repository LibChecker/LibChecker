package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.utils.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotTimeNodeItemsUseCase {

  suspend operator fun invoke(timeStamps: List<TimeStampItem>): Result = withContext(Dispatchers.Default) {
    val items = timeStamps.map { item ->
      SnapshotTimeNodeItem(
        timestamp = item.timestamp,
        topAppPackageNames = item.topApps.toPackageNames()
      )
    }
    Result(
      items = items,
      topAppPackageNames = items.asSequence()
        .flatMap { it.topAppPackageNames.asSequence() }
        .distinct()
        .toList()
    )
  }

  private fun String?.toPackageNames(): List<String> {
    return this?.fromJson<List<String>>(List::class.java, String::class.java).orEmpty()
  }

  data class Result(
    val items: List<SnapshotTimeNodeItem>,
    val topAppPackageNames: List<String>
  )
}
