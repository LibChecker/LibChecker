package com.absinthe.libchecker.domain.snapshot.comparison.model

import android.net.Uri

data class SnapshotComparisonInputs(
  val left: SnapshotComparisonInput = SnapshotComparisonInput(),
  val right: SnapshotComparisonInput = SnapshotComparisonInput()
) {

  val canCompare: Boolean
    get() = left.isSelected && right.isSelected

  val hasArchiveInput: Boolean
    get() = left.isArchive || right.isArchive

  fun selectSnapshot(side: SnapshotComparisonSide, timestamp: Long): SnapshotComparisonInputs {
    return replace(side, SnapshotComparisonInput.snapshot(timestamp))
  }

  fun selectArchive(side: SnapshotComparisonSide, uri: Uri): SnapshotComparisonInputs {
    return replace(side, SnapshotComparisonInput.archive(uri))
  }

  private fun replace(
    side: SnapshotComparisonSide,
    input: SnapshotComparisonInput
  ): SnapshotComparisonInputs {
    return when (side) {
      SnapshotComparisonSide.LEFT -> copy(left = input)
      SnapshotComparisonSide.RIGHT -> copy(right = input)
    }
  }
}

data class SnapshotComparisonInput(
  val timestamp: Long = UNSELECTED_TIMESTAMP,
  val uri: Uri? = null
) {

  val isSelected: Boolean
    get() = timestamp != UNSELECTED_TIMESTAMP

  val isArchive: Boolean
    get() = timestamp == ARCHIVE_TIMESTAMP

  val isSnapshot: Boolean
    get() = timestamp > UNSELECTED_TIMESTAMP

  companion object {
    fun snapshot(timestamp: Long): SnapshotComparisonInput {
      return SnapshotComparisonInput(timestamp = timestamp)
    }

    fun archive(uri: Uri): SnapshotComparisonInput {
      return SnapshotComparisonInput(
        timestamp = ARCHIVE_TIMESTAMP,
        uri = uri
      )
    }
  }
}

enum class SnapshotComparisonSide {
  LEFT,
  RIGHT;

  companion object {
    fun fromIsLeft(isLeft: Boolean): SnapshotComparisonSide {
      return if (isLeft) LEFT else RIGHT
    }
  }
}

private const val UNSELECTED_TIMESTAMP = 0L
private const val ARCHIVE_TIMESTAMP = -1L
