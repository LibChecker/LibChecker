package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo

sealed class PackageChangeState {
  data class Added(val packageInfo: PackageInfo) : PackageChangeState()
  data class Removed(val packageInfo: PackageInfo) : PackageChangeState()
  data class Replaced(val packageInfo: PackageInfo) : PackageChangeState()

  fun getActualPackageInfo(): PackageInfo {
    return when (this) {
      is Added -> this.packageInfo
      is Removed -> this.packageInfo
      is Replaced -> this.packageInfo
    }
  }
}
