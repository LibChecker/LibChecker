package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import com.blankj.utilcode.util.Utils

object PackageUtils {

    fun getPackageInfo(info: ApplicationInfo): PackageInfo {
        return Utils.getApp().packageManager.getPackageInfo(info.packageName, 0)
    }

    fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }
}