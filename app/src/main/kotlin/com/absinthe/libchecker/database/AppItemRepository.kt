package com.absinthe.libchecker.database

import android.content.pm.ApplicationInfo
import com.absinthe.libchecker.app.Global

object AppItemRepository {
  var allApplicationInfoMap: Map<String, ApplicationInfo> = HashMap(100)
  var trackItemsChanged = false
  var shouldRefreshAppList = false
  var shouldClearDiffItemsInDatabase = false

  suspend fun getApplicationInfoMap(): Map<String, ApplicationInfo> {
    Global.applicationListJob?.join()
    return allApplicationInfoMap
  }
}
