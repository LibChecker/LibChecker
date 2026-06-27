package com.absinthe.libchecker.features.album.comparison

import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.comparison.SnapshotComparisonInput
import java.io.File

internal object ComparisonDashboardStatePlanner {

  fun planSideState(
    input: SnapshotComparisonInput,
    formatTimestamp: (Long) -> String
  ): SideState {
    return when {
      input.isArchive -> SideState(
        timestampText = input.uri?.encodedPath?.toFileName(),
        appsCountText = ARCHIVE_APPS_COUNT
      )

      input.isSnapshot -> SideState(
        timestampText = formatTimestamp(input.timestamp),
        dashboardCountTimestamp = input.timestamp
      )

      else -> SideState()
    }
  }

  private fun String.toFileName(): String {
    return Uri.decode(this).substringAfterLast(File.separator)
  }

  data class SideState(
    val timestampText: String? = null,
    val appsCountText: String? = null,
    val dashboardCountTimestamp: Long? = null
  )

  private const val ARCHIVE_APPS_COUNT = "1"
}
