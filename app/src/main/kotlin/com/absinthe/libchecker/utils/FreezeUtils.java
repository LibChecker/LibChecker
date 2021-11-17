package com.absinthe.libchecker.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.ApplicationInfoHidden;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.absinthe.libchecker.SystemServices;

import dev.rikka.tools.refine.Refine;
import timber.log.Timber;

/**
 * Refer to com.catchingnow.icebox.sdk_client
 */
public class FreezeUtils {
  public static final int PM_FLAGS_GET_APP_INFO = VersionCompat.INSTANCE.getMATCH_UNINSTALLED_PACKAGES();
  private static final int PRIVATE_FLAG_HIDDEN = 1;
  private static final int FLAG_HIDDEN = 1 << 27;

  private static boolean isAppHidden(@NonNull ApplicationInfo ai) {
    try {
      int flags = Refine.<ApplicationInfoHidden>unsafeCast(ai).privateFlags;
      return (flags | PRIVATE_FLAG_HIDDEN) == flags;
    } catch (Throwable e) {
      return (ai.flags | FLAG_HIDDEN) == ai.flags;
    }
  }

  public static boolean isAppFrozen(@NonNull ApplicationInfo applicationInfo) {
    return isAppHidden(applicationInfo) || !applicationInfo.enabled;
  }

  public static boolean isAppFrozen(@NonNull String packageName) {
    PackageInfo packageInfo = null;
    try {
      packageInfo = SystemServices.INSTANCE.getPackageManager().getPackageInfo(
        packageName,
        FreezeUtils.PM_FLAGS_GET_APP_INFO | VersionCompat.INSTANCE.getMATCH_DISABLED_COMPONENTS()
      );
      return isAppFrozen(packageInfo.applicationInfo);
    } catch (PackageManager.NameNotFoundException e) {
      Timber.e(e);
    }
    return true;
  }
}
