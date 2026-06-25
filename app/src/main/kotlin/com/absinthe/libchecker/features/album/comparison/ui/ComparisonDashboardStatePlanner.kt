package com.absinthe.libchecker.features.album.comparison.ui

import android.net.Uri
import java.io.File

internal object ComparisonDashboardStatePlanner {

  fun planSideState(
    timestamp: Long,
    uri: Uri?,
    formatTimestamp: (Long) -> String
  ): SideState {
    return when {
      timestamp == ARCHIVE_TIMESTAMP -> SideState(
        timestampText = uri?.encodedPath?.toFileName(),
        appsCountText = ARCHIVE_APPS_COUNT
      )

      timestamp > 0 -> SideState(
        timestampText = formatTimestamp(timestamp),
        dashboardCountTimestamp = timestamp
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

  private const val ARCHIVE_TIMESTAMP = -1L
  private const val ARCHIVE_APPS_COUNT = "1"
}
