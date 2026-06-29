package com.absinthe.libchecker.domain.app.update

import com.absinthe.libchecker.api.bean.GetAppUpdateInfo

interface AppUpdateRepository {
  suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo?

  fun enqueueApkDownload(url: String): Long
}

enum class AppUpdateChannel {
  STABLE,
  CI
}
