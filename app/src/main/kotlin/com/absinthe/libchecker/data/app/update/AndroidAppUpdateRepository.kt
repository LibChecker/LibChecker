package com.absinthe.libchecker.data.app.update

import android.app.DownloadManager
import android.os.Environment
import androidx.core.net.toUri
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.api.request.GetAppUpdateRequest
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository

class AndroidAppUpdateRepository(
  private val downloadManager: DownloadManager,
  private val request: GetAppUpdateRequest = ApiManager.create()
) : AppUpdateRepository {

  override suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo? {
    return request.requestAppUpdateInfo(channel.requestValue)
  }

  override fun enqueueApkDownload(url: String): Long {
    val request = DownloadManager.Request(url.toUri()).apply {
      setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/"))
    }

    return downloadManager.enqueue(request)
  }

  private val AppUpdateChannel.requestValue: String
    get() = when (this) {
      AppUpdateChannel.STABLE -> "stable"
      AppUpdateChannel.CI -> "ci"
    }
}
