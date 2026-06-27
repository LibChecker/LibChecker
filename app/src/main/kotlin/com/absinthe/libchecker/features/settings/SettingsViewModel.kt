package com.absinthe.libchecker.features.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository
import com.absinthe.libchecker.domain.settings.BuildLogShareIntentUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
  private val appUpdateRepository: AppUpdateRepository,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val ruleSettingsRepository: RuleSettingsRepository,
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val buildLogShareIntentUseCase: BuildLogShareIntentUseCase
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

  fun setColorfulRuleIcon(enabled: Boolean) = viewModelScope.launch {
    appListSettingsRepository.notifyColorfulRuleIconChanged(enabled)
  }

  fun selectRemoteRulesRepository(repository: String) {
    ruleSettingsRepository.selectRemoteRepository(repository)
  }

  fun setSnapshotKeepRule(rule: String) {
    snapshotSettingsRepository.keepRule = rule
  }

  suspend fun buildLogShareIntent(): Result<Intent?> {
    return buildLogShareIntentUseCase()
  }
}
