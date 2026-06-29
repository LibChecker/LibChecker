package com.absinthe.libchecker.domain.app.detail.action

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppInfoPrimaryActionsUseCase(
  private val applicationId: String,
  private val getAppLaunchActionUseCase: GetAppLaunchActionUseCase
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
    return getAppLaunchActionUseCase(packageName)
      ?.let(AppInfoLaunchAction::Available)
      ?: AppInfoLaunchAction.Alternative
  }
}

data class AppInfoPrimaryActions(
  val launchAction: AppInfoLaunchAction,
  val settingsIntent: Intent
)

sealed interface AppInfoLaunchAction {
  data object Self : AppInfoLaunchAction
  data object Alternative : AppInfoLaunchAction
  data class Available(val action: AppLaunchAction) : AppInfoLaunchAction
}
