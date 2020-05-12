package com.absinthe.libchecker.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.Utils
import java.util.stream.Collectors


object PackageUtils {

    fun getPackageInfo(info: ApplicationInfo): PackageInfo {
        return Utils.getApp().packageManager.getPackageInfo(info.packageName, 0)
    }

    fun getPackageInfo(packageName: String): PackageInfo {
        return Utils.getApp().packageManager.getPackageInfo(packageName, 0)
    }

    fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    fun getVersionString(packageName: String): String {
        return try {
            val packageInfo: PackageInfo =
                Utils.getApp().packageManager.getPackageInfo(packageName, 0)
            "${packageInfo.versionName ?: "null"}(${getVersionCode(packageInfo)})"
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun getTargetApiString(packageName: String): String {
        return try {
            val packageInfo: PackageInfo =
                Utils.getApp().packageManager.getPackageInfo(packageName, 0)
            "API ${packageInfo.applicationInfo.targetSdkVersion}"
        } catch (e: PackageManager.NameNotFoundException) {
            "API ?"
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getInstalledApplications(context: Context): MutableList<ApplicationInfo> {
        return CacheFunctionUtil.get().staticCache(CacheFunctionUtil.Supplier {
            context.packageManager.getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
                .stream()
                .map { applicationInfo: ApplicationInfo -> applicationInfo }
                .collect(Collectors.toList())
        })
    }

}