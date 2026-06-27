package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.library.SnapshotLibrary
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection

class DeleteSnapshotTimeStampUseCase(
  private val snapshotLibrary: SnapshotLibrary,
  private val snapshotSelection: SnapshotSelection
) {

  suspend operator fun invoke(timestamp: Long): Result {
    snapshotLibrary.deleteTimeStamp(timestamp)
    val remainingTimeStamps = snapshotLibrary.getTimeStamps()
    snapshotSelection.selectLatestOrNone(remainingTimeStamps)
    return Result(
      remainingTimeStamps = remainingTimeStamps,
      selectedTimestamp = snapshotSelection.getCurrentTimestamp()
    )
  }

  data class Result(
    val remainingTimeStamps: List<TimeStampItem>,
    val selectedTimestamp: Long
  )
}
