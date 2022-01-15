package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfoHidden
import android.content.pm.PackageManager
import com.absinthe.libchecker.SystemServices
import dev.rikka.tools.refine.Refine
import timber.log.Timber

/**
 * Refer to com.catchingnow.icebox.sdk_client
 */
object FreezeUtils {
  val PM_FLAGS_GET_APP_INFO = VersionCompat.MATCH_UNINSTALLED_PACKAGES
  private const val PRIVATE_FLAG_HIDDEN = 1
  private const val FLAG_HIDDEN = 1 shl 27

  private fun isAppHidden(ai: ApplicationInfo): Boolean {
    return try {
      val flags: Int = Refine.unsafeCast<ApplicationInfoHidden>(ai).privateFlags
      flags or PRIVATE_FLAG_HIDDEN == flags
    } catch (e: Throwable) {
      ai.flags or FLAG_HIDDEN == ai.flags
    }
  }

  fun isAppFrozen(applicationInfo: ApplicationInfo): Boolean {
    return isAppHidden(applicationInfo) || !applicationInfo.enabled
  }

  fun isAppFrozen(packageName: String): Boolean {
    try {
      val packageInfo = SystemServices.packageManager.getPackageInfo(
        packageName,
        PM_FLAGS_GET_APP_INFO or VersionCompat.MATCH_DISABLED_COMPONENTS
      )
      return isAppFrozen(packageInfo.applicationInfo)
    } catch (e: PackageManager.NameNotFoundException) {
      Timber.e(e)
    }
    return true
  }
}
