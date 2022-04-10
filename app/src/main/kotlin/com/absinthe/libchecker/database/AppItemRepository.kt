package com.absinthe.libchecker.database

import android.content.pm.ApplicationInfo
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.utils.PackageUtils

object AppItemRepository {
  var allApplicationInfoMap: Map<String, ApplicationInfo> = HashMap(100)
  var trackItemsChanged = false
  var shouldRefreshAppList = false
  var shouldClearDiffItemsInDatabase = false

  suspend fun getApplicationInfoMap(): Map<String, ApplicationInfo> {
    Global.applicationListJob?.join()
    if (allApplicationInfoMap.isEmpty()) {
      allApplicationInfoMap = PackageUtils.getAppsList().asSequence()
        .map { it.packageName to it }
        .toMap()
    }
    return allApplicationInfoMap
  }
}
