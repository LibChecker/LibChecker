package com.absinthe.libchecker.domain.snapshot.comparison.model

internal data class ComparisonDashboardLabels(
  val timestampTitle: String,
  val chooseTimestampText: String,
  val appsCountTitle: String,
  val defaultAppsCountText: String
)

internal data class ComparisonDashboardSideState(
  val timestampText: String,
  val appsCountText: String,
  val contentDescription: String,
  val dashboardCountTimestamp: Long? = null
) {
  fun withAppsCountText(
    appsCountText: String,
    labels: ComparisonDashboardLabels
  ): ComparisonDashboardSideState {
    return copy(
      appsCountText = appsCountText,
      contentDescription = buildComparisonDashboardDescription(
        labels = labels,
        timestampText = timestampText,
        appsCountText = appsCountText
      )
    )
  }
}

internal data class ComparisonDashboardState(
  val left: ComparisonDashboardSideState,
  val right: ComparisonDashboardSideState
) {
  fun getSide(side: SnapshotComparisonSide): ComparisonDashboardSideState {
    return when (side) {
      SnapshotComparisonSide.LEFT -> left
      SnapshotComparisonSide.RIGHT -> right
    }
  }

  fun withAppsCountText(
    side: SnapshotComparisonSide,
    appsCountText: String,
    labels: ComparisonDashboardLabels
  ): ComparisonDashboardState {
    return when (side) {
      SnapshotComparisonSide.LEFT -> copy(
        left = left.withAppsCountText(appsCountText, labels)
      )

      SnapshotComparisonSide.RIGHT -> copy(
        right = right.withAppsCountText(appsCountText, labels)
      )
    }
  }
}

internal sealed interface ComparisonDashboardAction {
  data class SelectSide(
    val side: SnapshotComparisonSide
  ) : ComparisonDashboardAction
}

internal fun buildComparisonDashboardDescription(
  labels: ComparisonDashboardLabels,
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
