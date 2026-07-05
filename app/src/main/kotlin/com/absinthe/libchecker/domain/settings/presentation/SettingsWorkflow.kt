package com.absinthe.libchecker.domain.settings.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.app.list.export.ExportInstalledAppsToUriUseCase
import com.absinthe.libchecker.domain.app.repository.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.update.AppSelfUpdatePolicy
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.absinthe.libchecker.domain.rules.CloudRulesDownloadRequest
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository
import com.absinthe.libchecker.domain.settings.appearance.NightModeResolver
import com.absinthe.libchecker.domain.settings.model.GetUpdatesAction
import com.absinthe.libchecker.domain.settings.model.GetUpdatesItem
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceDisplayData
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceEntry
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceSummary
import com.absinthe.libchecker.domain.settings.repository.AppearanceSettingsRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.repository.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.usecase.UpdateLibReferenceThresholdUseCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SettingsWorkflow(
  private val context: Context,
  private val applicationId: String,
  private val apkAnalysisActivityClassName: String,
  private val appUpdateRepository: AppUpdateRepository,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val appearanceSettingsRepository: AppearanceSettingsRepository,
  private val cloudRulesRepository: CloudRulesRepository,
  private val ruleSettingsRepository: RuleSettingsRepository,
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val exportInstalledAppsToUriUseCase: ExportInstalledAppsToUriUseCase,
  private val libReferenceSettingsRepository: LibReferenceSettingsRepository,
  private val updateLibReferenceThresholdUseCase: UpdateLibReferenceThresholdUseCase
) {

  suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo? {
    return try {
      appUpdateRepository.requestUpdateInfo(channel)
        .also { Timber.d("requestUpdateSuccess: %s", it) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      Timber.e("requestUpdateFail: %s", e.stackTraceToString())
      null
    }
  }

  suspend fun installUpdate(url: String): AppUpdateInstallResult {
    return appUpdateRepository.installUpdate(url)
  }

  suspend fun hasAvailableAppUpdate(): Boolean {
    if (!AppSelfUpdatePolicy.isSelfUpdateEnabled(BuildConfig.IS_FOSS, BuildConfig.IS_DEV_VERSION)) {
      return false
    }
    return requestUpdateInfo(defaultUpdateChannel())?.appForFlavor(BuildConfig.IS_FOSS)?.versionCode
      ?.let { it > BuildConfig.VERSION_CODE } == true
  }

  suspend fun setColorfulRuleIcon(enabled: Boolean) {
    appListSettingsRepository.notifyColorfulRuleIconChanged(enabled)
  }

  fun setApkAnalysisEnabled(enabled: Boolean): Result<Unit> {
    val componentState = if (enabled) {
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    return runCatching {
      context.packageManager.setComponentEnabledSetting(
        ComponentName(applicationId, apkAnalysisActivityClassName),
        componentState,
        PackageManager.DONT_KILL_APP
      )
    }
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
    appearanceSettingsRepository.darkMode = darkMode
    return NightModeResolver.resolve(darkMode)
  }

  fun selectLocale(localeTag: String): Locale {
    appearanceSettingsRepository.localeTag = localeTag
    return if (localeTag == FOLLOW_SYSTEM_LOCALE) {
      Locale.getDefault()
    } else {
      Locale.forLanguageTag(localeTag)
    }
  }

  fun setSnapshotKeepRule(rule: String) {
    snapshotSettingsRepository.keepRule = rule
  }

  fun buildGetUpdatesItems(): List<GetUpdatesItem> {
    val items = listOf(
      GetUpdatesItem(
        text = "GitHub",
        iconRes = R.drawable.ic_github,
        action = GetUpdatesAction.OpenUri(URLManager.GITHUB_REPO_PAGE)
      ),
      GetUpdatesItem(
        text = "Google Play",
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_google,
        action = GetUpdatesAction.OpenUri(URLManager.PLAY_STORE_DETAIL_PAGE)
      ),
      GetUpdatesItem(
        text = "Telegram",
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_telegram,
        action = GetUpdatesAction.OpenUri(URLManager.TELEGRAM_RELEASES)
      ),
      GetUpdatesItem(
        text = "F-Droid",
        iconRes = R.drawable.ic_fdroid,
        action = GetUpdatesAction.OpenUri(URLManager.FDROID_PAGE)
      )
    )

    return if (AppSelfUpdatePolicy.isSelfUpdateEnabled(BuildConfig.IS_FOSS, BuildConfig.IS_DEV_VERSION)) {
      items + GetUpdatesItem(
        text = context.getString(R.string.settings_get_updates_in_app),
        iconRes = R.drawable.ic_logo,
        action = GetUpdatesAction.OpenInAppUpdate
      )
    } else {
      items
    }
  }

  private fun defaultUpdateChannel(): AppUpdateChannel {
    return if (BuildConfig.IS_DEV_VERSION) {
      AppUpdateChannel.CI
    } else {
      AppUpdateChannel.STABLE
    }
  }

  fun getLibReferenceThreshold(): Int {
    return libReferenceSettingsRepository.threshold
  }

  suspend fun setLibReferenceThreshold(threshold: Int) {
    updateLibReferenceThresholdUseCase(threshold)
  }

  fun buildLocalePreferenceData(
    entries: List<CharSequence>,
    entryValues: List<CharSequence>,
    selectedTag: String?
  ): LocalePreferenceDisplayData {
    val selectedIndex = entryValues.indexOfFirst { it.toString() == selectedTag }
    val userLocale = appearanceSettingsRepository.currentLocale
    val displayEntries = buildList {
      for (i in 1 until entries.size) {
        val locale = Locale.forLanguageTag(entries[i].toString())
        val localeName = locale.displayNameFor(locale)
        val localeNameUser = locale.displayNameFor(userLocale)
        add(
          LocalePreferenceEntry(
            index = i,
            label = if (selectedIndex != i) {
              "$localeName - $localeNameUser"
            } else {
              localeNameUser
            },
            selected = selectedIndex == i
          )
        )
      }
    }

    val summary = when {
      selectedTag.isNullOrEmpty() || selectedTag == FOLLOW_SYSTEM_LOCALE -> {
        LocalePreferenceSummary.FollowSystem
      }

      selectedIndex != -1 -> {
        LocalePreferenceSummary.LocaleName(displayEntries[selectedIndex - 1].label)
      }

      else -> LocalePreferenceSummary.Unchanged
    }

    return LocalePreferenceDisplayData(
      entries = displayEntries,
      summary = summary
    )
  }

  suspend fun buildLogShareIntent(): Result<Intent?> = withContext(Dispatchers.IO) {
    runCatching {
      val logDir = File(context.cacheDir, LOG_DIR_NAME)
      if (!logDir.exists() || !logDir.isDirectory) {
        return@runCatching null
      }

      val latestLogFile = logDir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(LOG_FILE_SUFFIX) }
        ?.maxByOrNull { it.lastModified() }
        ?: return@runCatching null

      Timber.d("Latest log file: ${latestLogFile.absolutePath}")
      val uri = FileProvider.getUriForFile(
        context,
        "$applicationId.fileprovider",
        latestLogFile
      )
      Intent(Intent.ACTION_SEND).apply {
        type = MIMETYPE_TEXT
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    }
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

  private fun Locale.displayNameFor(displayLocale: Locale): String {
    return if (script.isNotEmpty()) {
      getDisplayScript(displayLocale)
    } else {
      getDisplayName(displayLocale)
    }
  }

  private companion object {
    const val FOLLOW_SYSTEM_LOCALE = "SYSTEM"
    const val EXPORT_APPS_TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss"
    const val LOG_DIR_NAME = "logs"
    const val LOG_FILE_SUFFIX = ".log"
    const val MIMETYPE_TEXT = "text/plain"
  }
}
