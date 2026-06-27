package com.absinthe.libchecker.domain.settings.presentation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.list.export.ExportInstalledAppsToUriUseCase
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.absinthe.libchecker.domain.rules.CloudRulesDownloadRequest
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository
import com.absinthe.libchecker.domain.settings.model.GetUpdatesItem
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceDisplayData
import com.absinthe.libchecker.domain.settings.usecase.BuildGetUpdatesItemsUseCase
import com.absinthe.libchecker.domain.settings.usecase.BuildLocalePreferenceDataUseCase
import com.absinthe.libchecker.domain.settings.usecase.BuildLogShareIntentUseCase
import com.absinthe.libchecker.domain.settings.usecase.SelectDarkModeUseCase
import com.absinthe.libchecker.domain.settings.usecase.SelectLocaleUseCase
import com.absinthe.libchecker.domain.settings.usecase.SetApkAnalysisEnabledUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.repository.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.usecase.UpdateLibReferenceThresholdUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SettingsViewModel(
  private val appUpdateRepository: AppUpdateRepository,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val cloudRulesRepository: CloudRulesRepository,
  private val ruleSettingsRepository: RuleSettingsRepository,
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val buildGetUpdatesItemsUseCase: BuildGetUpdatesItemsUseCase,
  private val buildLocalePreferenceDataUseCase: BuildLocalePreferenceDataUseCase,
  private val buildLogShareIntentUseCase: BuildLogShareIntentUseCase,
  private val exportInstalledAppsToUriUseCase: ExportInstalledAppsToUriUseCase,
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

  suspend fun getCloudRulesVersionInfo(): CloudRulesVersionInfo? = withContext(Dispatchers.IO) {
    cloudRulesRepository.getVersionInfo()
  }

  fun getCloudRulesDownloadRequest(): CloudRulesDownloadRequest {
    return cloudRulesRepository.getDownloadRequest()
  }

  fun installDownloadedCloudRules(
    downloadRequest: CloudRulesDownloadRequest,
    remoteVersion: Int
  ): Boolean {
    return cloudRulesRepository.installDownloadedRules(downloadRequest, remoteVersion)
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

  fun buildGetUpdatesItems(): List<GetUpdatesItem> {
    return buildGetUpdatesItemsUseCase()
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

  fun buildInstalledAppsExportFileName(timestampMillis: Long = System.currentTimeMillis()): String {
    val timestamp = SimpleDateFormat(EXPORT_APPS_TIMESTAMP_PATTERN, Locale.US)
      .format(Date(timestampMillis))
    return "LibChecker-$timestamp.lcapps"
  }

  suspend fun exportInstalledApps(
    uri: Uri,
    progress: suspend (Int) -> Unit
  ): ExportInstalledAppsToUriUseCase.Result {
    return exportInstalledAppsToUriUseCase(uri, progress)
  }

  private companion object {
    const val EXPORT_APPS_TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss"
  }
}
