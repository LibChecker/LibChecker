package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo

data class InstalledPackageState(
  val packageInfo: PackageInfo?,
  val isFrozen: Boolean
)
