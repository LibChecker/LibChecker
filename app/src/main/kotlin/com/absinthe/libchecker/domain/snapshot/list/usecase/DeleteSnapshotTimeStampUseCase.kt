package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase

class DeleteSnapshotTimeStampUseCase(
  private val snapshotLibrary: SnapshotLibraryUseCase,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase
) {

  suspend operator fun invoke(timestamp: Long): Result {
    snapshotLibrary.deleteTimeStamp(timestamp)
    val remainingTimeStamps = snapshotLibrary.getTimeStamps()
    snapshotSelectionUseCase.selectLatestOrNone(remainingTimeStamps)
    return Result(
      remainingTimeStamps = remainingTimeStamps,
      selectedTimestamp = snapshotSelectionUseCase.getCurrentTimestamp()
    )
  }

  data class Result(
    val remainingTimeStamps: List<TimeStampItem>,
    val selectedTimestamp: Long
  )
}
