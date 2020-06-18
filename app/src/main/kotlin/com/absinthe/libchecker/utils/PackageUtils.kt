package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.system.ErrnoException
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.blankj.utilcode.util.Utils
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.collections.ArrayList


object PackageUtils {

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(info: ApplicationInfo, flag: Int = 0): PackageInfo {
        return Utils.getApp().packageManager.getPackageInfo(
            info.packageName,
            flag
        )
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(packageName: String, flag: Int = 0): PackageInfo {
        return Utils.getApp().packageManager.getPackageInfo(packageName, flag)
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
            "API ${getPackageInfo(packageName).applicationInfo.targetSdkVersion}"
        } catch (e: PackageManager.NameNotFoundException) {
            "API ?"
        }
    }

    fun getNativeDirLibs(sourcePath: String, nativePath: String): List<LibStringItem> {
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

            var splitName: String
            var next: ZipEntry

            while (entries.hasMoreElements()) {
                next = entries.nextElement()

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
        } catch (e: Exception) {
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
            val zipFile: ZipFile
            val entries: Enumeration<out ZipEntry>
            var next: ZipEntry

            for (file in it) {
                if (file.name.startsWith("split_config.arm")) {
                    zipFile = ZipFile(file)
                    entries = zipFile.entries()

                    while (entries.hasMoreElements()) {
                        next = entries.nextElement()

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

    fun getComponentList(
        packageName: String,
        type: LibReferenceActivity.Type
    ): List<String> {
        val flag = when (type) {
            LibReferenceActivity.Type.TYPE_SERVICE -> PackageManager.GET_SERVICES
            LibReferenceActivity.Type.TYPE_ACTIVITY -> PackageManager.GET_ACTIVITIES
            LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> PackageManager.GET_RECEIVERS
            LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> PackageManager.GET_PROVIDERS
            else -> 0
        }

        val packageInfo = getPackageInfo(packageName, flag)

        val list = when (flag) {
            PackageManager.GET_SERVICES -> packageInfo.services
            PackageManager.GET_ACTIVITIES -> packageInfo.activities
            PackageManager.GET_RECEIVERS -> packageInfo.receivers
            PackageManager.GET_PROVIDERS -> packageInfo.providers
            else -> null
        }

        val finalList = mutableListOf<String>()
        list?.let {
            for (component in it) {
                finalList.add(component.name)
            }
        }
        return finalList
    }

    fun getAbi(path: String, nativePath: String): Int {
        var abi = NO_LIBS

        try {
            val file = File(path)
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name

                if (name.contains("lib/")) {
                    if (name.contains("arm64-v8a")) {
                        abi = ARMV8
                    } else if (name.contains("armeabi-v7a")) {
                        if (abi != ARMV8) {
                            abi = ARMV7
                        }
                    } else if (name.contains("armeabi")) {
                        if (abi != ARMV8 && abi != ARMV7) {
                            abi = ARMV5
                        }
                    }
                }
            }
            zipFile.close()
            return if (abi == NO_LIBS) {
                getAbiByNativeDir(nativePath)
            } else {
                abi
            }
        } catch (e: ErrnoException) {
            e.printStackTrace()
            return ERROR
        } catch (e: ZipException) {
            e.printStackTrace()
            return ERROR
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return ERROR
        }
    }

    private fun getAbiByNativeDir(nativePath: String): Int {
        val file = File(nativePath.substring(0, nativePath.lastIndexOf("/")))
        val abiList = ArrayList<String>()

        val fileList = file.listFiles() ?: return NO_LIBS
        fileList.iterator().forEach { abiList.add(it.name) }

        return when {
            abiList.contains("arm64") -> ARMV8
            abiList.contains("arm") -> ARMV7
            else -> NO_LIBS
        }
    }
}