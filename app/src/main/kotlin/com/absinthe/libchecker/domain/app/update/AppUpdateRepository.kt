package com.absinthe.libchecker.domain.app.update

import com.absinthe.libchecker.api.bean.GetAppUpdateInfo

interface AppUpdateRepository {
  suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo?

  suspend fun installUpdate(url: String): AppUpdateInstallResult
}

enum class AppUpdateChannel {
  STABLE,
  CI
}

sealed interface AppUpdateInstallResult {
  data object Started : AppUpdateInstallResult
  data object Unsupported : AppUpdateInstallResult
  data class Failure(val message: String?) : AppUpdateInstallResult
}
