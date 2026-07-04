package com.absinthe.libchecker.domain.snapshot.comparison.presentation

import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInput
import java.io.File

internal object ComparisonDashboardStatePlanner {

  fun planSideState(
    input: SnapshotComparisonInput,
    labels: Labels,
    formatTimestamp: (Long) -> String
  ): SideState {
    return when {
      input.isArchive -> {
        val timestampText = input.uri?.encodedPath?.toFileName() ?: labels.chooseTimestampText
        SideState(
          timestampText = timestampText,
          appsCountText = ARCHIVE_APPS_COUNT,
          contentDescription = buildContentDescription(
            labels = labels,
            timestampText = timestampText,
            appsCountText = ARCHIVE_APPS_COUNT
          )
        )
      }

      input.isSnapshot -> {
        val timestampText = formatTimestamp(input.timestamp)
        SideState(
          timestampText = timestampText,
          appsCountText = labels.defaultAppsCountText,
          contentDescription = buildContentDescription(
            labels = labels,
            timestampText = timestampText,
            appsCountText = labels.defaultAppsCountText
          ),
          dashboardCountTimestamp = input.timestamp
        )
      }

      else -> SideState(
        timestampText = labels.chooseTimestampText,
        appsCountText = labels.defaultAppsCountText,
        contentDescription = buildContentDescription(
          labels = labels,
          timestampText = labels.chooseTimestampText,
          appsCountText = labels.defaultAppsCountText
        )
      )
    }
  }

  private fun buildContentDescription(
    labels: Labels,
    timestampText: String,
    appsCountText: String
  ): String {
    return listOf(
      labels.timestampTitle,
      timestampText,
      labels.appsCountTitle,
      appsCountText
    )
      .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
      .joinToString()
  }

  private fun String.toFileName(): String {
    return Uri.decode(this).substringAfterLast(File.separator)
  }

  data class Labels(
    val timestampTitle: String,
    val chooseTimestampText: String,
    val appsCountTitle: String,
    val defaultAppsCountText: String
  )

  data class SideState(
    val timestampText: String,
    val appsCountText: String,
    val contentDescription: String,
    val dashboardCountTimestamp: Long? = null
  ) {
    fun withAppsCountText(
      appsCountText: String,
      labels: Labels
    ): SideState {
      return copy(
        appsCountText = appsCountText,
        contentDescription = buildContentDescription(
          labels = labels,
          timestampText = timestampText,
          appsCountText = appsCountText
        )
      )
    }
  }

  private const val ARCHIVE_APPS_COUNT = "1"
}
