package com.absinthe.libchecker.domain.settings.presentation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.list.export.ExportInstalledAppsToUriUseCase
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.rules.CloudRulesDownloadRequest
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.settings.model.GetUpdatesItem
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceDisplayData
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val settingsWorkflow: SettingsWorkflow
) : ViewModel() {
  private val _respStateFlow: MutableSharedFlow<AppUpdateInfoResult> = MutableSharedFlow()
  val respStateFlow: SharedFlow<AppUpdateInfoResult> = _respStateFlow.asSharedFlow()
  private val _updateBadgeVisible = MutableStateFlow(false)
  val updateBadgeVisible: StateFlow<Boolean> = _updateBadgeVisible.asStateFlow()
  private var requestUpdateJob: Job? = null
  private var checkUpdateJob: Job? = null

  fun requestUpdate(channel: AppUpdateChannel) {
    requestUpdateJob?.cancel()
    requestUpdateJob = viewModelScope.launch(Dispatchers.IO) {
      _respStateFlow.emit(
        AppUpdateInfoResult(
          channel = channel,
          updateInfo = settingsWorkflow.requestUpdateInfo(channel)
        )
      )
    }
  }

  fun checkForUpdates() {
    if (checkUpdateJob?.isActive == true) {
      return
    }
    checkUpdateJob = viewModelScope.launch(Dispatchers.IO) {
      _updateBadgeVisible.value = settingsWorkflow.hasAvailableAppUpdate()
    }
  }

  suspend fun installUpdate(url: String): AppUpdateInstallResult {
    return settingsWorkflow.installUpdate(url)
  }

  fun setColorfulRuleIcon(enabled: Boolean) = viewModelScope.launch {
    settingsWorkflow.setColorfulRuleIcon(enabled)
  }

  fun setApkAnalysisEnabled(enabled: Boolean): Result<Unit> {
    return settingsWorkflow.setApkAnalysisEnabled(enabled)
  }

  fun selectRemoteRulesRepository(repository: String) {
    settingsWorkflow.selectRemoteRulesRepository(repository)
  }

  suspend fun getCloudRulesVersionInfo(): CloudRulesVersionInfo? {
    return settingsWorkflow.getCloudRulesVersionInfo()
  }

  fun getCloudRulesDownloadRequest(): CloudRulesDownloadRequest {
    return settingsWorkflow.getCloudRulesDownloadRequest()
  }

  fun installDownloadedCloudRules(
    downloadRequest: CloudRulesDownloadRequest,
    remoteVersion: Int
  ): Boolean {
    return settingsWorkflow.installDownloadedCloudRules(downloadRequest, remoteVersion)
  }

  fun selectDarkMode(darkMode: String): Int {
    return settingsWorkflow.selectDarkMode(darkMode)
  }

  fun selectLocale(localeTag: String): Locale {
    return settingsWorkflow.selectLocale(localeTag)
  }

  fun setSnapshotKeepRule(rule: String) {
    settingsWorkflow.setSnapshotKeepRule(rule)
  }

  fun buildGetUpdatesItems(): List<GetUpdatesItem> {
    return settingsWorkflow.buildGetUpdatesItems()
  }

  fun getLibReferenceThreshold(): Int {
    return settingsWorkflow.getLibReferenceThreshold()
  }

  fun setLibReferenceThreshold(threshold: Int) = viewModelScope.launch {
    settingsWorkflow.setLibReferenceThreshold(threshold)
  }

  fun buildLocalePreferenceData(
    entries: List<CharSequence>,
    entryValues: List<CharSequence>,
    selectedTag: String?
  ): LocalePreferenceDisplayData {
    return settingsWorkflow.buildLocalePreferenceData(
      entries = entries,
      entryValues = entryValues,
      selectedTag = selectedTag
    )
  }

  suspend fun buildLogShareIntent(): Result<Intent?> {
    return settingsWorkflow.buildLogShareIntent()
  }

  fun buildInstalledAppsExportFileName(timestampMillis: Long = System.currentTimeMillis()): String {
    return settingsWorkflow.buildInstalledAppsExportFileName(timestampMillis)
  }

  suspend fun exportInstalledApps(
    uri: Uri,
    progress: suspend (Int) -> Unit
  ): ExportInstalledAppsToUriUseCase.Result {
    return settingsWorkflow.exportInstalledApps(uri, progress)
  }
}

data class AppUpdateInfoResult(
  val channel: AppUpdateChannel,
  val updateInfo: GetAppUpdateInfo?
)
