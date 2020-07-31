package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.blankj.utilcode.util.Utils
import net.dongliu.apk.parser.ApkFile
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
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
        val packageInfo = Utils.getApp().packageManager.getPackageInfo(
            packageName, FreezeUtils.PM_FLAGS_GET_APP_INFO
        )
        if (FreezeUtils.isAppFrozen(packageInfo.applicationInfo)) {
            val pmFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_DISABLED_COMPONENTS
            } else {
                PackageManager.GET_DISABLED_COMPONENTS
            }
            return Utils.getApp().packageManager.getPackageArchiveInfo(
                Utils.getApp().packageManager.getPackageInfo(
                    packageInfo.packageName,
                    0
                ).applicationInfo.sourceDir, pmFlag or flag
            ) ?: throw PackageManager.NameNotFoundException()
        }
        return Utils.getApp().packageManager.getPackageInfo(packageName, flag)
    }

    fun getInstallApplications(): List<ApplicationInfo> {
        return try {
            Utils.getApp().packageManager
                .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    fun getVersionString(packageInfo: PackageInfo): String {
        return try {
            "${packageInfo.versionName ?: "null"}(${getVersionCode(packageInfo)})"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    fun getTargetApiString(packageInfo: PackageInfo): String {
        return try {
            "API ${packageInfo.applicationInfo.targetSdkVersion}"
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

    fun isSplitsApk(packageInfo: PackageInfo): Boolean {
        try {
            val path = packageInfo.applicationInfo.sourceDir
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

    fun isKotlinUsed(packageInfo: PackageInfo): Boolean {
        try {
            val path = packageInfo.applicationInfo.sourceDir
            val file = File(path)
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()
            var next: ZipEntry

            while (entries.hasMoreElements()) {
                next = entries.nextElement()

                when {
                    next.name.startsWith("kotlin/") -> {
                        return true
                    }
                    next.name.startsWith("META-INF/services/kotlin") -> {
                        return true
                    }
                }
            }
            zipFile.close()
            return isKotlinUsedInClassDex(file)
        } catch (e: Exception) {
            return false
        }
    }

    fun isKotlinUsedInClassDex(file: File): Boolean {
        try {
            val apkFile = ApkFile(file)

            for (dexClass in apkFile.dexClasses) {
                if (dexClass.toString().startsWith("Lkotlin/") || dexClass.toString()
                        .startsWith("Lkotlinx/")
                ) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun getSplitLibs(path: String): ArrayList<LibStringItem> {
        val libList = ArrayList<LibStringItem>()

        File(path.substring(0, path.lastIndexOf("/"))).listFiles()?.let {
            var zipFile: ZipFile
            var entries: Enumeration<out ZipEntry>
            var next: ZipEntry

            for (file in it) {
                if (file.name.startsWith("split_config.arm")) {
                    try {
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
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        }

        return libList
    }

    fun getComponentList(
        packageName: String,
        type: LibReferenceActivity.Type,
        isSimpleName: Boolean
    ): List<String> {
        val flag = when (type) {
            LibReferenceActivity.Type.TYPE_SERVICE -> PackageManager.GET_SERVICES
            LibReferenceActivity.Type.TYPE_ACTIVITY -> PackageManager.GET_ACTIVITIES
            LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> PackageManager.GET_RECEIVERS
            LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> PackageManager.GET_PROVIDERS
            else -> 0
        }

        return getComponentList(getPackageInfo(packageName, flag), type, isSimpleName)
    }

    fun getComponentList(
        packageInfo: PackageInfo,
        type: LibReferenceActivity.Type,
        isSimpleName: Boolean
    ): List<String> {
        val flag = when (type) {
            LibReferenceActivity.Type.TYPE_SERVICE -> PackageManager.GET_SERVICES
            LibReferenceActivity.Type.TYPE_ACTIVITY -> PackageManager.GET_ACTIVITIES
            LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> PackageManager.GET_RECEIVERS
            LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> PackageManager.GET_PROVIDERS
            else -> 0
        }

        val list: Array<out ComponentInfo>? = when (flag) {
            PackageManager.GET_SERVICES -> packageInfo.services
            PackageManager.GET_ACTIVITIES -> packageInfo.activities
            PackageManager.GET_RECEIVERS -> packageInfo.receivers
            PackageManager.GET_PROVIDERS -> packageInfo.providers
            else -> null
        }

        val finalList = mutableListOf<String>()
        list?.let {
            for (component in it) {
                val name = if (isSimpleName) {
                    component.name.removePrefix(packageInfo.packageName)
                } else {
                    component.name
                }
                finalList.add(name)
            }
        }

        return finalList
    }

    fun getComponentList(
        packageName: String,
        list: Array<out ComponentInfo>,
        isSimpleName: Boolean
    ): List<String> {
        val finalList = mutableListOf<String>()
        for (component in list) {
            val name = if (isSimpleName) {
                component.name.removePrefix(packageName)
            } else {
                component.name
            }
            finalList.add(name)
        }

        return finalList
    }

    fun getAbi(path: String, nativePath: String, isApk: Boolean = false): Int {
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
            return if (abi == NO_LIBS && !isApk) {
                getAbiByNativeDir(nativePath)
            } else {
                abi
            }
        } catch (e: Exception) {
            return ERROR
        }
    }

    fun getAbiString(abi: Int): String {
        return when (abi) {
            ARMV8 -> ARMV8_STRING
            ARMV7 -> ARMV7_STRING
            ARMV5 -> ARMV5_STRING
            NO_LIBS -> Utils.getApp().getString(R.string.no_libs)
            ERROR -> "Can\'t read"
            else -> "Unknown"
        }
    }

    fun getAbiBadgeResource(type: Int): Int {
        return when (type) {
            ARMV8 -> R.drawable.ic_64bit
            ARMV7, ARMV5 -> R.drawable.ic_32bit
            else -> R.drawable.ic_no_libs
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

    fun sizeToString(size: Long): String {
        return "(${Formatter.formatFileSize(Utils.getApp(), size)})"
    }
}