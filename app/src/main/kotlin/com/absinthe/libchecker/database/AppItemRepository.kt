package com.absinthe.libchecker.database

import android.content.pm.ApplicationInfo
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.database.entity.RuleEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object AppItemRepository {
  var allApplicationInfoItems: List<ApplicationInfo> = emptyList()
  var trackItemsChanged = false
  var shouldRefreshAppList = false
  var rulesRegexList = ConcurrentHashMap<Pattern, RuleEntity>()
  var shouldClearDiffItemsInDatabase = false

  suspend fun getApplicationInfoItems(): List<ApplicationInfo> {
    Global.applicationListJob?.join()
    return allApplicationInfoItems
  }
}
