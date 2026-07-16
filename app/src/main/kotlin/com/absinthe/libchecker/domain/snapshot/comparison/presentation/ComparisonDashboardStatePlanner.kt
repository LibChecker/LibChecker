package com.absinthe.libchecker.domain.snapshot.comparison.presentation

import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardLabels
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardSideState
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardState
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInput
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInputs
import com.absinthe.libchecker.domain.snapshot.comparison.model.buildComparisonDashboardDescription
import java.io.File

internal object ComparisonDashboardStatePlanner {

  fun planState(
    inputs: SnapshotComparisonInputs,
    labels: ComparisonDashboardLabels,
    formatTimestamp: (Long) -> String
  ): ComparisonDashboardState {
    return ComparisonDashboardState(
      left = planSideState(inputs.left, labels, formatTimestamp),
      right = planSideState(inputs.right, labels, formatTimestamp)
    )
  }

  fun planSideState(
    input: SnapshotComparisonInput,
    labels: ComparisonDashboardLabels,
    formatTimestamp: (Long) -> String
  ): ComparisonDashboardSideState {
    return when {
      input.isArchive -> {
        val timestampText = input.uri?.encodedPath?.toFileName() ?: labels.chooseTimestampText
        ComparisonDashboardSideState(
          timestampText = timestampText,
          appsCountText = ARCHIVE_APPS_COUNT,
          contentDescription = buildComparisonDashboardDescription(
            labels = labels,
            timestampText = timestampText,
            appsCountText = ARCHIVE_APPS_COUNT
          )
        )
      }

      input.isSnapshot -> {
        val timestampText = formatTimestamp(input.timestamp)
        ComparisonDashboardSideState(
          timestampText = timestampText,
          appsCountText = labels.defaultAppsCountText,
          contentDescription = buildComparisonDashboardDescription(
            labels = labels,
            timestampText = timestampText,
            appsCountText = labels.defaultAppsCountText
          ),
          dashboardCountTimestamp = input.timestamp
        )
      }

      else -> ComparisonDashboardSideState(
        timestampText = labels.chooseTimestampText,
        appsCountText = labels.defaultAppsCountText,
        contentDescription = buildComparisonDashboardDescription(
          labels = labels,
          timestampText = labels.chooseTimestampText,
          appsCountText = labels.defaultAppsCountText
        )
      )
    }
  }

  private fun String.toFileName(): String {
    return Uri.decode(this).substringAfterLast(File.separator)
  }

  private const val ARCHIVE_APPS_COUNT = "1"
}
