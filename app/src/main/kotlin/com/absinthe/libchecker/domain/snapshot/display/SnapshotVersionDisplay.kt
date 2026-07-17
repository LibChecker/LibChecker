package com.absinthe.libchecker.domain.snapshot.display

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

internal fun SnapshotDiffItem.buildSnapshotVersionDisplayDiff(
  archivedLabel: String
): SnapshotDiffItem.DiffNode<String> {
  val old = buildSnapshotVersionDisplayText(
    isArchived = archivedDiff.old,
    versionName = versionNameDiff.old,
    versionCode = versionCodeDiff.old,
    archivedLabel = archivedLabel
  )
  val hasNewValue = archivedDiff.new != null || versionNameDiff.new != null || versionCodeDiff.new != null
  val new = if (hasNewValue) {
    buildSnapshotVersionDisplayText(
      isArchived = archivedDiff.new ?: false,
      versionName = versionNameDiff.new,
      versionCode = versionCodeDiff.new,
      archivedLabel = archivedLabel
    )
  } else {
    null
  }
  return SnapshotDiffItem.DiffNode(old, new)
}

private fun buildSnapshotVersionDisplayText(
  isArchived: Boolean,
  versionName: String?,
  versionCode: Long?,
  archivedLabel: String
): String {
  if (isArchived) {
    return versionCode?.let { "$archivedLabel ($it)" } ?: archivedLabel
  }

  val displayVersionName = versionName?.takeUnless {
    it.isBlank() || it.equals("null", ignoreCase = true)
  }
  return when {
    displayVersionName != null && versionCode != null -> "$displayVersionName ($versionCode)"
    displayVersionName != null -> displayVersionName
    versionCode != null -> versionCode.toString()
    else -> ""
  }
}
