package com.absinthe.libchecker.domain.app.list.model

import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants

data class AppListItemIconDisplay(
  val packageInfo: PackageInfo?,
  val usePackageIcon: Boolean
) {

  companion object {
    fun create(
      packageName: String,
      packageInfo: PackageInfo?
    ): AppListItemIconDisplay {
      val usePackageIcon = packageName != Constants.EXAMPLE_PACKAGE && packageInfo != null
      return AppListItemIconDisplay(
        packageInfo = packageInfo.takeIf { usePackageIcon },
        usePackageIcon = usePackageIcon
      )
    }
  }
}
