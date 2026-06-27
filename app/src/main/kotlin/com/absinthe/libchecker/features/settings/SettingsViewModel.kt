package com.absinthe.libchecker.features.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.SetApkAnalysisEnabledUseCase
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository
import com.absinthe.libchecker.domain.settings.BuildLocalePreferenceDataUseCase
import com.absinthe.libchecker.domain.settings.BuildLogShareIntentUseCase
import com.absinthe.libchecker.domain.settings.LocalePreferenceDisplayData
import com.absinthe.libchecker.domain.settings.SelectDarkModeUseCase
import com.absinthe.libchecker.domain.settings.SelectLocaleUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.UpdateLibReferenceThresholdUseCase
import java.util.Locale
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
  private val buildLocalePreferenceDataUseCase: BuildLocalePreferenceDataUseCase,
  private val buildLogShareIntentUseCase: BuildLogShareIntentUseCase,
  private val selectDarkModeUseCase: SelectDarkModeUseCase,
  private val selectLocaleUseCase: SelectLocaleUseCase,
  private val setApkAnalysisEnabledUseCase: SetApkAnalysisEnabledUseCase,
  private val libReferenceSettingsRepository: LibReferenceSettingsRepository,
  private val updateLibReferenceThresholdUseCase: UpdateLibReferenceThresholdUseCase
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

  fun setApkAnalysisEnabled(enabled: Boolean): Result<Unit> {
    return setApkAnalysisEnabledUseCase(enabled)
  }

  fun selectRemoteRulesRepository(repository: String) {
    ruleSettingsRepository.selectRemoteRepository(repository)
  }

  fun selectDarkMode(darkMode: String): Int {
    return selectDarkModeUseCase(darkMode)
  }

  fun selectLocale(localeTag: String): Locale {
    return selectLocaleUseCase(localeTag)
  }

  fun setSnapshotKeepRule(rule: String) {
    snapshotSettingsRepository.keepRule = rule
  }

  fun getLibReferenceThreshold(): Int {
    return libReferenceSettingsRepository.threshold
  }

  fun setLibReferenceThreshold(threshold: Int) = viewModelScope.launch {
    updateLibReferenceThresholdUseCase(threshold)
  }

  fun buildLocalePreferenceData(
    entries: List<CharSequence>,
    entryValues: List<CharSequence>,
    selectedTag: String?
  ): LocalePreferenceDisplayData {
    return buildLocalePreferenceDataUseCase(
      entries = entries,
      entryValues = entryValues,
      selectedTag = selectedTag
    )
  }

  suspend fun buildLogShareIntent(): Result<Intent?> {
    return buildLogShareIntentUseCase()
  }
}
