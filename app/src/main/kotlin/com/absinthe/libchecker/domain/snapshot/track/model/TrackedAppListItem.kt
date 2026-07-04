package com.absinthe.libchecker.domain.snapshot.track.model

import android.content.pm.PackageInfo

data class TrackedAppListItem(
  val packageInfo: PackageInfo,
  val label: String,
  val packageName: String,
  val description: String,
  val switchState: Boolean = false
)

fun buildTrackedAppListItemDescription(
  label: CharSequence?,
  packageName: CharSequence?
): String {
  return listOf(label, packageName)
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}
