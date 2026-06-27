package com.absinthe.libchecker.domain.snapshot.track.model

import android.content.pm.PackageInfo

data class TrackedAppListItem(
  val packageInfo: PackageInfo,
  val label: String,
  val packageName: String,
  val switchState: Boolean = false
)
