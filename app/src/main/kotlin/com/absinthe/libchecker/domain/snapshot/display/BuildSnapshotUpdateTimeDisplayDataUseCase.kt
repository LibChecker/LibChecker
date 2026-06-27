package com.absinthe.libchecker.domain.snapshot.display

import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.extensions.PREINSTALLED_TIMESTAMP
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BuildSnapshotUpdateTimeDisplayDataUseCase {

  operator fun invoke(request: Request): SnapshotUpdateTimeDisplayData? {
    if (!request.isVisible) {
      return null
    }
    return SnapshotUpdateTimeDisplayData(
      text = if (request.updateTime <= PREINSTALLED_TIMESTAMP) {
        SnapshotUpdateTimeText.Preinstalled
      } else {
        SnapshotUpdateTimeText.LastUpdated(formatUpdateTime(request.updateTime))
      },
      isApexPackage = request.isApexPackage
    )
  }

  private fun formatUpdateTime(timestamp: Long): String {
    val pattern = if (DateUtils.isTimestampToday(timestamp)) {
      TODAY_PATTERN
    } else {
      FULL_PATTERN
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
  }

  data class Request(
    val updateTime: Long,
    val isVisible: Boolean,
    val isApexPackage: Boolean
  )

  private companion object {
    private const val FULL_PATTERN = "yyyy-MM-dd HH:mm:ss"
    private const val TODAY_PATTERN = "HH:mm:ss"
  }
}

data class SnapshotUpdateTimeDisplayData(
  val text: SnapshotUpdateTimeText,
  val isApexPackage: Boolean
)

sealed interface SnapshotUpdateTimeText {
  data object Preinstalled : SnapshotUpdateTimeText
  data class LastUpdated(val timeText: String) : SnapshotUpdateTimeText
}
