package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.constant.*
import com.absinthe.libchecker.extensions.logd
import com.blankj.utilcode.util.Utils
import net.dongliu.apk.parser.ApkFile
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.collections.ArrayList
import kotlin.jvm.Throws

object PackageUtils {

    /**
     * Get packageInfo
     * @param info ApplicationInfo
     * @param flag Flag mask
     * @return PackageInfo
     * @throws PackageManager.NameNotFoundException
     */
    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(info: ApplicationInfo, flag: Int = 0): PackageInfo {
        return getPackageInfo(info.packageName, flag)
    }

    /**
     * Get packageInfo
     * @param packageName Package name string
     * @param flag Flag mask
     * @return PackageInfo
     * @throws PackageManager.NameNotFoundException
     */
    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(packageName: String, flag: Int = 0): PackageInfo {
        val packageInfo = Utils.getApp().packageManager.getPackageInfo(
            packageName, FreezeUtils.PM_FLAGS_GET_APP_INFO or flag
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
        return packageInfo
    }

    /**
     * Get all installed apps in device
     * @return list of apps
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getInstallApplications(): List<ApplicationInfo> {
        return try {
            val timer = TimeRecorder()
            timer.start()
            val list= ActivityStackManager.topActivity!!.packageManager.getInstalledApplications(0)
            timer.end()
            logd("getInstallApplications: $timer")
            list
        } catch (e: Exception) {
            throw Exception()
        }
    }

    /**
     * Get version code of an app
     * @param packageInfo PackageInfo
     * @return version code as Long Integer
     */
    fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * Get version string of an app ( 1.0.0(1) )
     * @param packageInfo PackageInfo
     * @return version code as String
     */
    fun getVersionString(packageInfo: PackageInfo): String {
        return try {
            "${packageInfo.versionName ?: "null"}(${getVersionCode(packageInfo)})"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    /**
     * Get version string of an app ( 1.0.0(1) )
     * @param versionName Version name
     * @param versionCode Version code
     * @return version code as String
     */
    fun getVersionString(versionName: String, versionCode: Long): String {
        return "${versionName}(${versionCode})"
    }

    /**
     * Get target api string of an app ( API 30 )
     * @param packageInfo PackageInfo
     * @return version code as String
     */
    fun getTargetApiString(packageInfo: PackageInfo): String {
        return try {
            "API ${packageInfo.applicationInfo.targetSdkVersion}"
        } catch (e: PackageManager.NameNotFoundException) {
            "API ?"
        }
    }

    /**
     * Get native libraries of an app
     * @param sourcePath Source path of the app
     * @param nativePath Native library path of the app
     * @return List of LibStringItem
     */
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

    /**
     * Get native libraries of an app from source path
     * @param path Source path of the app
     * @return List of LibStringItem
     */
    private fun getSourceLibs(path: String): ArrayList<LibStringItem> {
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

    /**
     * Get native libraries of an app from split apk
     * @param path Source path of the app
     * @return List of LibStringItem
     */
    private fun getSplitLibs(path: String): ArrayList<LibStringItem> {
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

    /**
     * Judge that whether an app uses split apks
     * @param packageInfo PackageInfo
     * @return true if it uses split apks
     */
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
        } catch (e: Exception) {
            return false
        }

        return false
    }

    /**
     * Judge that whether an app uses Kotlin language
     * @param packageInfo PackageInfo
     * @return true if it uses Kotlin language
     */
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

    /**
     * Judge that whether an app uses Kotlin language from classes.dex
     * @param file APK file of the app
     * @return true if it uses Kotlin language
     */
    private fun isKotlinUsedInClassDex(file: File): Boolean {
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

    /**
     * Get components list of an app
     * @param packageName Package name of the app
     * @param type Component type
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of String
     */
    fun getComponentList(
        packageName: String,
        @LibType type: Int,
        isSimpleName: Boolean
    ): List<String> {
        val flag = when (type) {
            SERVICE -> PackageManager.GET_SERVICES
            ACTIVITY -> PackageManager.GET_ACTIVITIES
            RECEIVER -> PackageManager.GET_RECEIVERS
            PROVIDER -> PackageManager.GET_PROVIDERS
            else -> 0
        }

        return getComponentList(getPackageInfo(packageName, flag), type, isSimpleName)
    }

    /**
     * Get components list of an app
     * @param packageInfo PackageInfo
     * @param type Component type
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of String
     */
    private fun getComponentList(
        packageInfo: PackageInfo,
        @LibType type: Int,
        isSimpleName: Boolean
    ): List<String> {
        val list: Array<out ComponentInfo>? = when (type) {
            SERVICE -> packageInfo.services
            ACTIVITY -> packageInfo.activities
            RECEIVER -> packageInfo.receivers
            PROVIDER -> packageInfo.providers
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

    /**
     * Get components list of an app
     * @param packageName Package name of the app
     * @param list List of components
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of String
     */
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

    /**
     * Get ABI type of an app
     * @param path Source path of the app
     * @param nativePath Native path of the app
     * @param isApk Whether is an APK file
     * @return ABI type
     */
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

    /**
     * Get ABI type of an app from native path
     * @param nativePath Native path of the app
     * @return ABI type
     */
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

    /**
     * Get ABI string from ABI type
     * @param abi ABI type
     * @return ABI string
     */
    fun getAbiString(abi: Int): String {
        return when (abi) {
            ARMV8 -> ARMV8_STRING
            ARMV7 -> ARMV7_STRING
            ARMV5 -> ARMV5_STRING
            NO_LIBS -> Utils.getApp().getString(R.string.no_libs)
            ERROR -> Utils.getApp().getString(R.string.cannot_read)
            else -> Utils.getApp().getString(R.string.unknown)
        }
    }

    /**
     * Get ABI badge resource from ABI type
     * @param type ABI type
     * @return Badge resource
     */
    fun getAbiBadgeResource(type: Int): Int {
        return when (type) {
            ARMV8 -> R.drawable.ic_64bit
            ARMV7, ARMV5 -> R.drawable.ic_32bit
            else -> R.drawable.ic_no_libs
        }
    }

    /**
     * Format size number to string
     * @param size Size of file
     * @return String of size number (100KB)
     */
    fun sizeToString(size: Long): String {
        return "(${Formatter.formatFileSize(Utils.getApp(), size)})"
    }

    fun getDexList(packageName: String, isApk: Boolean = false): List<LibStringItem> {
        val packageInfo: PackageInfo
        val map = mutableMapOf<String, Int>()
        val list = mutableListOf<LibStringItem>()

        try {
            packageInfo = getPackageInfo(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return listOf()
        }

        if (!isApk) {
            val path = packageInfo.applicationInfo.sourceDir
            val apkFile = ApkFile(File(path))

            var splits: List<String>
            var key: String

            apkFile.dexClasses.forEach {
                splits = it.classType.removePrefix("L").split("/")

                key = if (splits.size <= 3) {
                    "${splits[0]}.${splits[1]}"
                } else {
                    "${splits[0]}.${splits[1]}.${splits[2]}"
                }

                map[key]?.let { value ->
                    map[key] = value + 1
                } ?: let {
                    map[key] = 0
                }
            }

            for (item in map) {
                list.add(LibStringItem(item.key, item.value.toLong()))
            }
            return list.sortedBy { it.name }
        } else {
            return listOf()
        }
    }
}