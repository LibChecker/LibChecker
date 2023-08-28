package com.absinthe.libchecker.dev.exception

import com.absinthe.libchecker.BuildConfig

class AppListIncompleteException(message: String?) : Exception(message) {
  companion object {
    fun toggleAndSubmit(
      appList: Collection<String>,
      newApps: Collection<String>,
      deletedApps: Collection<String>
    ) {
      if (BuildConfig.IS_DEV_VERSION.not()) return
      runCatching {
        val msg = buildString {
          append("Large diff detected in app list. ")
          appendLine()
          append("Total: ${appList.size}")
          appendLine()
          append("New: ${newApps.size}")
          appendLine()
          append("Deleted: ${deletedApps.size}")
          appendLine()
          append("Same: ${newApps.filter { deletedApps.contains(it) }.size}")
        }
        throw AppListIncompleteException(msg)
      }
    }
  }
}
