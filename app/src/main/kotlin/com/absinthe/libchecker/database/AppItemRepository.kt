package com.absinthe.libchecker.database

import android.content.pm.PackageInfo
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.utils.PackageUtils

object AppItemRepository {
  var allPackageInfoMap: Map<String, PackageInfo> = HashMap(100)
  var trackItemsChanged = false
  var shouldClearDiffItemsInDatabase = false

  suspend fun getApplicationInfoMap(): Map<String, PackageInfo> {
    Global.applicationListJob?.join()
    if (allPackageInfoMap.isEmpty()) {
      allPackageInfoMap = PackageUtils.getAppsList().asSequence()
        .map { it.packageName to it }
        .toMap()
    }
    return allPackageInfoMap
  }
}
