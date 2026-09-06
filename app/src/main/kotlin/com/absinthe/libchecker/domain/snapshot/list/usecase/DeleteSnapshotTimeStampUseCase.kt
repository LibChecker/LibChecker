package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection

class DeleteSnapshotTimeStampUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val snapshotSelection: SnapshotSelection
) {

  suspend operator fun invoke(timestamp: Long): Result {
    snapshotRepository.deleteSnapshotsAndTimeStamp(timestamp)
    val remainingTimeStamps = snapshotRepository.getTimeStamps()
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
