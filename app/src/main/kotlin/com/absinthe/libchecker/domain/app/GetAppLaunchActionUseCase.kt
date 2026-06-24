package com.absinthe.libchecker.domain.app

import android.content.Intent
import com.absinthe.libchecker.compat.PackageManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppLaunchActionUseCase {

  suspend operator fun invoke(packageName: String?): AppLaunchAction? =
    withContext(Dispatchers.IO) {
      if (packageName.isNullOrBlank()) {
        return@withContext null
      }
      val launcherActivity = getLauncherActivity(packageName).takeIf(String::isNotBlank)
        ?: return@withContext null
      AppLaunchAction(
        launcherActivity = launcherActivity,
        intent = Intent(Intent.ACTION_MAIN)
          .addCategory(Intent.CATEGORY_LAUNCHER)
          .setClassName(packageName, launcherActivity)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }

  private fun getLauncherActivity(packageName: String): String {
    val intent = Intent(Intent.ACTION_MAIN, null)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    return PackageManagerCompat.queryIntentActivities(intent, 0)
      .firstOrNull()
      ?.activityInfo
      ?.name
      .orEmpty()
  }
}

data class AppLaunchAction(
  val launcherActivity: String,
  val intent: Intent
)
