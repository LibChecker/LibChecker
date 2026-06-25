package com.absinthe.libchecker.domain.snapshot

class BuildSnapshotComparisonPlanUseCase(
  private val buildSnapshotComparisonLists: BuildSnapshotComparisonListsUseCase
) {

  suspend operator fun invoke(
    leftTimeStamp: Long,
    leftArchive: ArchiveSnapshotItem?,
    rightTimeStamp: Long,
    rightArchive: ArchiveSnapshotItem?
  ): SnapshotComparisonPlan? {
    return when {
      leftTimeStamp > 0 && rightTimeStamp > 0 -> {
        if (leftTimeStamp == rightTimeStamp) {
          null
        } else {
          SnapshotComparisonPlan.TimestampRange(
            previousTimestamp = leftTimeStamp.coerceAtMost(rightTimeStamp),
            currentTimestamp = leftTimeStamp.coerceAtLeast(rightTimeStamp)
          )
        }
      }

      leftArchive != null && rightArchive != null -> {
        val requiresDifferentPackageConfirmation =
          leftArchive.snapshotItem.packageName != rightArchive.snapshotItem.packageName

        SnapshotComparisonPlan.ArchivePair(
          left = leftArchive,
          right = rightArchive,
          requiresDifferentPackageConfirmation = requiresDifferentPackageConfirmation
        )
      }

      else -> {
        buildSnapshotComparisonLists(
          leftTimeStamp = leftTimeStamp,
          leftPackage = leftArchive?.snapshotItem,
          rightTimeStamp = rightTimeStamp,
          rightPackage = rightArchive?.snapshotItem
        )?.let(SnapshotComparisonPlan::SnapshotLists)
      }
    }
  }
}

sealed interface SnapshotComparisonPlan {

  data class TimestampRange(
    val previousTimestamp: Long,
    val currentTimestamp: Long
  ) : SnapshotComparisonPlan

  data class ArchivePair(
    val left: ArchiveSnapshotItem,
    val right: ArchiveSnapshotItem,
    val requiresDifferentPackageConfirmation: Boolean
  ) : SnapshotComparisonPlan

  data class SnapshotLists(
    val lists: SnapshotComparisonLists
  ) : SnapshotComparisonPlan
}
