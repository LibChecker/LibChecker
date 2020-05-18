package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.absinthe.libchecker.bean.LibStringItem
import com.blankj.utilcode.util.Utils
import java.io.File
import java.util.zip.ZipFile


object PackageUtils {

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(info: ApplicationInfo): PackageInfo {
        return Utils.getApp().packageManager.getPackageInfo(info.packageName, 0)
    }

    @Throws(PackageManager.NameNotFoundException::class)
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

    fun getAbiByNativeDir(sourcePath: String, nativePath: String): List<LibStringItem> {
        val file = File(nativePath)
        val list = ArrayList<LibStringItem>()

        file.listFiles()?.let {
            for (abi in it) {
                list.add(LibStringItem(abi.name, abi.length()))
            }
        }

        if (list.isEmpty()) {
            list.addAll(getSourceLibs(sourcePath))
        }

        return list
    }

    fun getSourceLibs(path: String): ArrayList<LibStringItem> {
        val file = File(path)
        val zipFile = ZipFile(file)
        val entries = zipFile.entries()
        val libList = ArrayList<LibStringItem>()

        while (entries.hasMoreElements()) {
            val next = entries.nextElement()

            if (next.name.contains("lib/")) {
                libList.add(LibStringItem(next.name.split("/").last(), next.size))
            }
        }
        zipFile.close()

        return libList
    }
}