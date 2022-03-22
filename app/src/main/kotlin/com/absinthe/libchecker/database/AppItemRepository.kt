package com.absinthe.libchecker.database

import android.content.pm.ApplicationInfo
import com.absinthe.libchecker.app.Global

object AppItemRepository {
  var allApplicationInfoItems: List<ApplicationInfo> = emptyList()
  var trackItemsChanged = false
  var shouldRefreshAppList = false
  var shouldClearDiffItemsInDatabase = false

  suspend fun getApplicationInfoItems(): List<ApplicationInfo> {
    Global.applicationListJob?.join()
    return allApplicationInfoItems
  }
}
