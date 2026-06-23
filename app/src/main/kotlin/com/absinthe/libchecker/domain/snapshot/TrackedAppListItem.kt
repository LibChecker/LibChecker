package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageInfo

data class TrackedAppListItem(
  val packageInfo: PackageInfo,
  val label: String,
  val packageName: String,
  var switchState: Boolean = false
)
