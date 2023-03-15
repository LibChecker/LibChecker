package com.absinthe.libchecker.database

import android.content.pm.PackageInfo
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.utils.PackageUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object AppItemRepository {
  var allPackageInfoMap: ConcurrentMap<String, PackageInfo> = ConcurrentHashMap(100)
  var trackItemsChanged = false

  suspend fun getApplicationInfoMap(): Map<String, PackageInfo> {
    Global.applicationListJob?.join()
    if (allPackageInfoMap.isEmpty()) {
      allPackageInfoMap.clear()
      allPackageInfoMap.putAll(
        PackageUtils.getAppsList().asSequence()
          .map { it.packageName to it }
          .toMap(),
      )
    }
    return allPackageInfoMap
  }
}
