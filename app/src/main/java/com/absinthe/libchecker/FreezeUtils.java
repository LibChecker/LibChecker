package com.absinthe.libchecker;

import android.content.pm.ApplicationInfo;

import androidx.annotation.Nullable;

import com.absinthe.libchecker.compat.VersionCompat;

import java.lang.reflect.Field;

/**
 * Refer to com.catchingnow.icebox.sdk_client
 */
public class FreezeUtils {
    @Nullable
    private static Field AI_FIELD;

    static {
        try {
            //noinspection JavaReflectionMemberAccess
            AI_FIELD = ApplicationInfo.class.getDeclaredField("privateFlags");
            AI_FIELD.setAccessible(true);
        } catch (Throwable ignored) {
            AI_FIELD = null;
        }
    }

    private static final int PRIVATE_FLAG_HIDDEN = 1;
    private static final int FLAG_HIDDEN = 1 << 27;
    public static final int PM_FLAGS_GET_APP_INFO = VersionCompat.INSTANCE.getMATCH_UNINSTALLED_PACKAGES();

    private static boolean isAppHidden(ApplicationInfo ai) {
        if (AI_FIELD != null) {
            try {
                int flags = (int) AI_FIELD.get(ai);
                return (flags | PRIVATE_FLAG_HIDDEN) == flags;
            } catch (Throwable e) {
                return (ai.flags | FLAG_HIDDEN) == ai.flags;
            }
        } else {
            return (ai.flags | FLAG_HIDDEN) == ai.flags;
        }
    }

    public static boolean isAppFrozen(ApplicationInfo applicationInfo) {
        return isAppHidden(applicationInfo) || !applicationInfo.enabled;
    }
}
