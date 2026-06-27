package com.absinthe.libchecker.domain.app.list.model

import android.content.pm.PackageInfo

data class InstalledPackageState(
  val packageInfo: PackageInfo?,
  val isFrozen: Boolean
)
