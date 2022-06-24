package com.absinthe.libchecker.database

import android.content.pm.PackageInfo
import androidx.collection.ArrayMap
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.utils.PackageUtils

object AppItemRepository {
  var allPackageInfoMap: MutableMap<String, PackageInfo> = ArrayMap(100)
  var trackItemsChanged = false

  suspend fun getApplicationInfoMap(): Map<String, PackageInfo> {
    Global.applicationListJob?.join()
    if (allPackageInfoMap.isEmpty()) {
      allPackageInfoMap.clear()
      allPackageInfoMap.putAll(
        PackageUtils.getAppsList().asSequence()
          .map { it.packageName to it }
          .toMap()
      )
    }
    return allPackageInfoMap
  }
}
