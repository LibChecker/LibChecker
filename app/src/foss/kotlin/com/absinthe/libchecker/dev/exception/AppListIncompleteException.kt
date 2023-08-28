package com.absinthe.libchecker.dev.exception

class AppListIncompleteException(message: String?) : Exception(message) {
  companion object {
    fun toggleAndSubmit(
      appList: Collection<String>,
      newApps: Collection<String>,
      deletedApps: Collection<String>
    ) {
    }
  }
}
