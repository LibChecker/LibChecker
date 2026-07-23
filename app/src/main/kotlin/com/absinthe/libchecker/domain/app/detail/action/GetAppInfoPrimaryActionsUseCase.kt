package com.absinthe.libchecker.domain.app.detail.action

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.absinthe.libchecker.compat.PackageManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppInfoPrimaryActionsUseCase(
  private val applicationId: String
) {

  suspend operator fun invoke(packageName: String?): AppInfoPrimaryActions = withContext(Dispatchers.IO) {
    AppInfoPrimaryActions(
      launchAction = getLaunchAction(packageName),
      settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  private suspend fun getLaunchAction(packageName: String?): AppInfoLaunchAction {
    if (packageName == applicationId) {
      return AppInfoLaunchAction.Self
    }
    return getAppLaunchAction(packageName)
      ?.let(AppInfoLaunchAction::Available)
      ?: AppInfoLaunchAction.Alternative
  }

  suspend fun getAppLaunchAction(packageName: String?): AppLaunchAction? = withContext(Dispatchers.IO) {
    if (packageName.isNullOrBlank()) {
      return@withContext null
    }
    val intent = Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    val launcherActivity = PackageManagerCompat.queryIntentActivities(intent, 0)
      .firstOrNull()
      ?.activityInfo
      ?.name
      .orEmpty()
      .takeIf(String::isNotBlank)
      ?: return@withContext null
    AppLaunchAction(
      launcherActivity = launcherActivity,
      intent = intent
        .setClassName(packageName, launcherActivity)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }
}

data class AppLaunchAction(
  val launcherActivity: String,
  val intent: Intent
)

data class AppInfoPrimaryActions(
  val launchAction: AppInfoLaunchAction,
  val settingsIntent: Intent
)

sealed interface AppInfoLaunchAction {
  data object Self : AppInfoLaunchAction
  data object Alternative : AppInfoLaunchAction
  data class Available(val action: AppLaunchAction) : AppInfoLaunchAction
}
