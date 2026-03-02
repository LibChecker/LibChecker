package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfoHidden
import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.PackageManagerCompat
import dev.rikka.tools.refine.Refine
import timber.log.Timber

/**
 * Refer to com.catchingnow.icebox.sdk_client
 */
object FreezeUtils {
  private const val FLAG_HIDDEN = 1 shl 27

  private fun isAppHidden(ai: ApplicationInfo): Boolean {
    return try {
      val flags: Int = Refine.unsafeCast<ApplicationInfoHidden>(ai).privateFlags
      flags or ApplicationInfoHidden.PRIVATE_FLAG_HIDDEN == flags
    } catch (_: Throwable) {
      ai.flags or FLAG_HIDDEN == ai.flags
    }
  }

  fun isAppFrozen(applicationInfo: ApplicationInfo): Boolean {
    return isAppHidden(applicationInfo) || !applicationInfo.enabled
  }

  fun isAppFrozen(packageName: String): Boolean {
    runCatching {
      val packageInfo = PackageManagerCompat.getPackageInfo(
        packageName,
        PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
      )
      return isAppFrozen(packageInfo.applicationInfo!!)
    }.onFailure {
      Timber.e(it)
    }
    return true
  }
}
