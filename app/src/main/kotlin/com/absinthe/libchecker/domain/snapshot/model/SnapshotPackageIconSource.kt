package com.absinthe.libchecker.domain.snapshot.model

import android.content.pm.PackageInfo

sealed interface SnapshotPackageIconSource {
  data class InstalledPackage(val packageInfo: PackageInfo) : SnapshotPackageIconSource
  data object Fallback : SnapshotPackageIconSource
}
