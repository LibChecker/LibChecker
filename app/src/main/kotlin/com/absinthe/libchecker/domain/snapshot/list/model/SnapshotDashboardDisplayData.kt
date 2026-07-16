package com.absinthe.libchecker.domain.snapshot.list.model

data class SnapshotDashboardDisplayData(
  val timestampText: CharSequence,
  val appsCountText: CharSequence,
  val systemProps: List<SnapshotSystemPropDisplayData>,
  val contentDescription: String,
  val timestampContentDescription: String
)

sealed interface SnapshotDashboardAction {
  data object OpenAlbum : SnapshotDashboardAction
  data object ChangeTimestamp : SnapshotDashboardAction
}

fun buildSnapshotDashboardDescription(
  timestampTitle: CharSequence,
  timestampText: CharSequence,
  appsCountTitle: CharSequence,
  appsCountText: CharSequence
): String {
  return buildSnapshotDashboardDescriptionParts(
    timestampTitle,
    timestampText,
    appsCountTitle,
    appsCountText
  )
}

fun buildSnapshotDashboardTimestampDescription(
  actionTitle: CharSequence,
  timestampText: CharSequence
): String {
  return buildSnapshotDashboardDescriptionParts(actionTitle, timestampText)
}

private fun buildSnapshotDashboardDescriptionParts(vararg parts: CharSequence?): String {
  return parts
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}
