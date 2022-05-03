@file:Suppress("DEPRECATION")

package com.absinthe.libchecker.utils

import android.content.pm.PackageManager

object VersionCompat {
  val MATCH_DISABLED_COMPONENTS = if (OsUtils.atLeastN()) {
    PackageManager.MATCH_DISABLED_COMPONENTS
  } else {
    PackageManager.GET_DISABLED_COMPONENTS
  }

  val MATCH_UNINSTALLED_PACKAGES = if (OsUtils.atLeastN()) {
    PackageManager.MATCH_UNINSTALLED_PACKAGES
  } else {
    PackageManager.GET_UNINSTALLED_PACKAGES
  }
}
