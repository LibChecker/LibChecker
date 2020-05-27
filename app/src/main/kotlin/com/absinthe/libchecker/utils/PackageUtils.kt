package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.provider.ContextProvider
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile


object PackageUtils {

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(info: ApplicationInfo): PackageInfo {
        return ContextProvider.getGlobalContext().packageManager.getPackageInfo(info.packageName, 0)
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(packageName: String): PackageInfo {
        return ContextProvider.getGlobalContext().packageManager.getPackageInfo(packageName, 0)
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
                ContextProvider.getGlobalContext().packageManager.getPackageInfo(packageName, 0)
            "${packageInfo.versionName ?: "null"}(${getVersionCode(packageInfo)})"
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun getTargetApiString(packageName: String): String {
        return try {
            val packageInfo: PackageInfo =
                ContextProvider.getGlobalContext().packageManager.getPackageInfo(packageName, 0)
            "API ${packageInfo.applicationInfo.targetSdkVersion}"
        } catch (e: PackageManager.NameNotFoundException) {
            "API ?"
        }
    }

    fun getAbiByNativeDir(sourcePath: String, nativePath: String): List<LibStringItem> {
        val file = File(nativePath)
        val list = ArrayList<LibStringItem>()

        file.listFiles()?.let { fileList ->
            for (abi in fileList) {
                if (!list.any { it.name == abi.name }) {
                    list.add(LibStringItem(abi.name, abi.length()))
                }
            }
        }

        if (list.isEmpty()) {
            list.addAll(getSourceLibs(sourcePath))
        }

        return list
    }

    fun getSourceLibs(path: String): ArrayList<LibStringItem> {
        val libList = ArrayList<LibStringItem>()

        try {
            val file = File(path)
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()
            var splitName = ""

            while (entries.hasMoreElements()) {
                val next = entries.nextElement()

                if (next.name.contains("lib/")) {
                    splitName = next.name.split("/").last()

                    if (!libList.any { it.name == splitName }) {
                        libList.add(LibStringItem(splitName, next.size))
                    }
                }
            }
            zipFile.close()

            if (libList.isEmpty()) {
                libList.addAll(getSplitLibs(path))
            }

            return libList
        } catch (e: ZipException) {
            e.printStackTrace()
            return libList
        }
    }

    fun isSplitsApk(packageName: String): Boolean {
        try {
            val path = getPackageInfo(packageName).applicationInfo.sourceDir
            File(path.substring(0, path.lastIndexOf("/"))).listFiles()?.let {
                for (file in it) {
                    if (file.name.startsWith("split_config.")) {
                        return true
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

        return false
    }

    fun getSplitLibs(path: String): ArrayList<LibStringItem> {
        val libList = ArrayList<LibStringItem>()

        File(path.substring(0, path.lastIndexOf("/"))).listFiles()?.let {
            for (file in it) {
                if (file.name.startsWith("split_config.arm")) {
                    val zipFile = ZipFile(file)
                    val entries = zipFile.entries()

                    while (entries.hasMoreElements()) {
                        val next = entries.nextElement()
                        if (next.name.contains("lib/") && !next.isDirectory) {
                            libList.add(LibStringItem(next.name.split("/").last(), next.size))
                        }
                    }
                    zipFile.close()

                    break
                }
            }
        }

        return libList
    }
}