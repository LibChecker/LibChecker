package com.absinthe.libchecker.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
  private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {
  private val _respStateFlow: MutableSharedFlow<GetAppUpdateInfo?> = MutableSharedFlow()
  val respStateFlow: SharedFlow<GetAppUpdateInfo?> = _respStateFlow.asSharedFlow()

  fun requestUpdate(isStableChannel: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    val channel = if (isStableChannel) {
      AppUpdateChannel.STABLE
    } else {
      AppUpdateChannel.CI
    }
    val resp = runCatching {
      appUpdateRepository.requestUpdateInfo(channel)
    }.onFailure { Timber.e("requestUpdateFail: %s", it.stackTraceToString()) }
      .onSuccess { Timber.d("requestUpdateSuccess: %s", it) }
      .getOrNull()
    _respStateFlow.emit(resp)
  }

  fun downloadApk(url: String) = viewModelScope.launch(Dispatchers.IO) {
    appUpdateRepository.enqueueApkDownload(url)
  }
}
