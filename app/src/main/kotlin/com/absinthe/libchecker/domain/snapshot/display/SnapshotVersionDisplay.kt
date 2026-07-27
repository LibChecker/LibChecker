package com.absinthe.libchecker.domain.snapshot.display

import android.content.Context
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.extensions.sizeToString
import java.util.Locale
import kotlin.math.abs

internal fun formatSnapshotSizeChange(
  context: Context,
  diffSize: Long,
  oldSize: Long
): String = buildString {
  if (diffSize > 0) {
    append("+")
  }
  append(diffSize.sizeToString(context))
  append(", ")
  if (diffSize > 0) {
    append("+")
  }
  append(formatSnapshotSizePercentage(diffSize.toFloat() / oldSize))
}

private fun formatSnapshotSizePercentage(percentage: Float): String {
  if (abs(percentage) < 0.001f) {
    return if (percentage < 0) "-<0.1%" else "<0.1%"
  }
  return String.format(Locale.getDefault(), "%.1f%%", percentage * 100)
}

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
