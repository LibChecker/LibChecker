package com.absinthe.libchecker.dev.exception

import androidx.annotation.Keep
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle

@Keep
class AppListIncompleteException(message: String?) : Exception(message) {
  companion object {
    fun toggleAndSubmit(
      appList: Collection<String>,
      newApps: Collection<String>,
      deletedApps: Collection<String>
    ) {
      doOnMainThreadIdle {
        if (BuildConfig.IS_DEV_VERSION.not()) return@doOnMainThreadIdle
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
}
