package com.absinthe.libchecker.features.settings

import android.app.DownloadManager
import android.os.Environment
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.api.request.GetAppUpdateRequest
import com.absinthe.libchecker.app.SystemServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel : ViewModel() {
  private val request: GetAppUpdateRequest = ApiManager.create()
  private val _respStateFlow: MutableSharedFlow<GetAppUpdateInfo?> = MutableSharedFlow()
  val respStateFlow: SharedFlow<GetAppUpdateInfo?> = _respStateFlow.asSharedFlow()

  fun requestUpdate(isStableChannel: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    val resp = runCatching {
      request.requestAppUpdateInfo(if (isStableChannel) "stable" else "ci")
    }.onFailure { Timber.e("requestUpdateFail: %s", it.stackTraceToString()) }
      .onSuccess { Timber.d("requestUpdateSuccess: %s", it) }
      .getOrNull()
    _respStateFlow.emit(resp)
  }

  fun downloadApk(url: String) = viewModelScope.launch(Dispatchers.IO) {
    val request = DownloadManager.Request(url.toUri()).apply {
      setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/"))
    }

    SystemServices.downloadManager.enqueue(request)
  }
}
