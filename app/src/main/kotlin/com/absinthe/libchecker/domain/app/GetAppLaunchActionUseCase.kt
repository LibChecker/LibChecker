package com.absinthe.libchecker.domain.app

import android.content.Intent
import com.absinthe.libchecker.compat.PackageManagerCompat

class GetAppLaunchActionUseCase {

  operator fun invoke(packageName: String?): AppLaunchAction? {
    if (packageName.isNullOrBlank()) {
      return null
    }
    val launcherActivity = getLauncherActivity(packageName).takeIf(String::isNotBlank)
      ?: return null
    return AppLaunchAction(
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
