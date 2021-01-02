package com.absinthe.libchecker.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.format.Formatter
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.StatefulComponent
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV5_STRING
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV7_STRING
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.ARMV8_STRING
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.constant.Constants.X86_64_STRING
import com.absinthe.libchecker.constant.Constants.X86_STRING
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.DexLibMap
import com.absinthe.libchecker.exception.MiuiOpsException
import com.absinthe.libchecker.extensions.loge
import com.absinthe.libchecker.java.FreezeUtils
import com.absinthe.libraries.utils.utils.XiaomiUtilities
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.Utils
import net.dongliu.apk.parser.ApkFile
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.collections.ArrayList

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
        val pmFlag = if (LCAppUtils.atLeastN()) {
            PackageManager.MATCH_DISABLED_COMPONENTS
        } else {
            PackageManager.GET_DISABLED_COMPONENTS
        }
        val packageInfo = Utils.getApp().packageManager.getPackageInfo(
            packageName, FreezeUtils.PM_FLAGS_GET_APP_INFO or flag or pmFlag
        )
        if (FreezeUtils.isAppFrozen(packageInfo.applicationInfo)) {
            val info = Utils.getApp().packageManager.getPackageInfo(packageInfo.packageName, 0)

            return Utils.getApp().packageManager.getPackageArchiveInfo(info.applicationInfo.sourceDir, pmFlag or flag)?.apply {
                applicationInfo.sourceDir = info.applicationInfo.sourceDir
                applicationInfo.nativeLibraryDir = info.applicationInfo.nativeLibraryDir
            } ?: throw PackageManager.NameNotFoundException()
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
        if (!XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_GET_INSTALLED_APPS)) {
            throw MiuiOpsException("miui: not permitted OP_GET_INSTALLED_APPS")
        }

        return try {
            Utils.getApp().packageManager?.getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES) ?: emptyList()
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
        return if (LCAppUtils.atLeastP()) {
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
        val list = file.listFiles()?.let { list ->
            list.asSequence()
                .distinctBy { it.name }
                .map { LibStringItem(it.name, it.length()) }
                .toMutableList()
        } ?: mutableListOf()

        if (list.isEmpty()) {
            list.addAll(getSourceLibs(sourcePath, "lib/"))
        }
        list.addAll(getSourceLibs(sourcePath, "assets/", "/assets"))

        return list.distinctBy { it.name }
    }

    /**
     * Get native libraries of an app from source path
     * @param path Source path of the app
     * @return List of LibStringItem
     */
    private fun getSourceLibs(path: String, childDir: String, source: String? = null): List<LibStringItem> {
        var zipFile: ZipFile? = null

        try {
            val file = File(path)
            zipFile = ZipFile(file)
            val entries = zipFile.entries()
            val libList = entries.asSequence()
                .filter { (it.name.contains(childDir)) && it.name.endsWith(".so") }
                .distinctBy { it.name.split("/").last() }
                .map { LibStringItem(it.name.split("/").last(), it.size, source) }
                .toList()

            if (libList.isEmpty()) {
                return getSplitLibs(path)
            }

            return libList
        } catch (e: Exception) {
            loge(e.toString())
            return emptyList()
        } finally {
            zipFile?.close()
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
            var zipFile: ZipFile? = null
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
                        break
                    } catch (e: Exception) {
                        continue
                    } finally {
                        zipFile?.close()
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
                if (it.any { item -> item.name.startsWith("split_config.") }) {
                    return true
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
        var zipFile: ZipFile? = null

        return try {
            val path = packageInfo.applicationInfo.sourceDir
            val file = File(path)
            zipFile = ZipFile(file)

            if (zipFile.entries().asSequence().any { it.name.startsWith("kotlin/") || it.name.startsWith("META-INF/services/kotlin") }) {
                true
            } else {
                isKotlinUsedInClassDex(file)
            }
        } catch (e: Exception) {
            false
        } finally {
            zipFile?.close()
        }
    }

    /**
     * Judge that whether an app uses Kotlin language from classes.dex
     * @param file APK file of the app
     * @return true if it uses Kotlin language
     */
    private fun isKotlinUsedInClassDex(file: File): Boolean {
        return try {
            ApkFile(file).dexClasses.asSequence().any { it.toString().startsWith("Lkotlin/") || it.toString().startsWith("Lkotlinx/") }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get components list of an app
     * @param packageName Package name of the app
     * @param type Component type
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of StatefulComponent
     */
    fun getComponentList(packageName: String, @LibType type: Int, isSimpleName: Boolean): List<StatefulComponent> {
        val flag = when (type) {
            SERVICE -> PackageManager.GET_SERVICES
            ACTIVITY -> PackageManager.GET_ACTIVITIES
            RECEIVER -> PackageManager.GET_RECEIVERS
            PROVIDER -> PackageManager.GET_PROVIDERS
            else -> 0
        }

        return try {
            getComponentList(getPackageInfo(packageName, flag), type, isSimpleName)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get components list of an app
     * @param packageName Package name of the app
     * @param type Component type
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of String
     */
    fun getComponentStringList(packageName: String, @LibType type: Int, isSimpleName: Boolean): List<String> {
        val flag = when (type) {
            SERVICE -> PackageManager.GET_SERVICES
            ACTIVITY -> PackageManager.GET_ACTIVITIES
            RECEIVER -> PackageManager.GET_RECEIVERS
            PROVIDER -> PackageManager.GET_PROVIDERS
            else -> 0
        }

        return try {
            getComponentStringList(getPackageInfo(packageName, flag), type, isSimpleName)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get components list of an app
     * @param packageInfo PackageInfo
     * @param type Component type
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of StatefulComponent
     */
    private fun getComponentList(packageInfo: PackageInfo, @LibType type: Int, isSimpleName: Boolean): List<StatefulComponent> {
        val list: Array<out ComponentInfo>? = when (type) {
            SERVICE -> packageInfo.services
            ACTIVITY -> packageInfo.activities
            RECEIVER -> packageInfo.receivers
            PROVIDER -> packageInfo.providers
            else -> null
        }

        return getComponentList(packageInfo.packageName, list, isSimpleName)
    }

    /**
     * Get components list of an app
     * @param packageInfo PackageInfo
     * @param type Component type
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of String
     */
    private fun getComponentStringList(packageInfo: PackageInfo, @LibType type: Int, isSimpleName: Boolean): List<String> {
        val list: Array<out ComponentInfo>? = when (type) {
            SERVICE -> packageInfo.services
            ACTIVITY -> packageInfo.activities
            RECEIVER -> packageInfo.receivers
            PROVIDER -> packageInfo.providers
            else -> null
        }

        return getComponentStringList(packageInfo.packageName, list, isSimpleName)
    }

    /**
     * Get components list of an app
     * @param packageName Package name of the app
     * @param list List of components(can be nullable)
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of StatefulComponent
     */
    fun getComponentList(packageName: String, list: Array<out ComponentInfo>?, isSimpleName: Boolean): List<StatefulComponent> {
        if (list.isNullOrEmpty()) {
            return emptyList()
        }
        return list.asSequence()
            .map {
                if (isSimpleName) {
                    StatefulComponent(it.name.removePrefix(packageName), it.enabled)
                } else {
                    StatefulComponent(it.name, it.enabled)
                }
            }
            .toList()
    }

    /**
     * Get components list of an app
     * @param packageName Package name of the app
     * @param list List of components(can be nullable)
     * @param isSimpleName Whether to show class name as a simple name
     * @return List of String
     */
    fun getComponentStringList(packageName: String, list: Array<out ComponentInfo>?, isSimpleName: Boolean): List<String> {
        if (list.isNullOrEmpty()) {
            return emptyList()
        }
        return list.asSequence()
            .map {
                if (isSimpleName) {
                    it.name.removePrefix(packageName)
                } else {
                    it.name
                }
            }
            .toList()
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
        var elementName: String

        val file = File(path)
        val zipFile = ZipFile(file)
        val entries = zipFile.entries()

        try {
            while (entries.hasMoreElements()) {
                elementName = entries.nextElement().name

                if (elementName.contains("lib/")) {
                    if (elementName.contains(ARMV8_STRING)) {
                        if (GlobalValues.deviceSupportedAbis.contains(ARMV8_STRING)) {
                            abi = ARMV8
                        }
                        break
                    } else if (elementName.contains(ARMV7_STRING)) {
                        if (GlobalValues.deviceSupportedAbis.contains(ARMV7_STRING)) {
                            abi = ARMV7
                        }
                    } else if (elementName.contains(ARMV5_STRING)) {
                        if (GlobalValues.deviceSupportedAbis.contains(ARMV5_STRING) && abi != ARMV7) {
                            abi = ARMV5
                        }
                    } else if (elementName.contains(X86_64_STRING)) {
                        if (GlobalValues.deviceSupportedAbis.contains(X86_64_STRING) && GlobalValues.deviceSupportedAbis.none { it.startsWith("arm") }) {
                            abi = X86_64
                        }
                    } else if (elementName.contains(X86_STRING)) {
                        if (GlobalValues.deviceSupportedAbis.contains(X86_STRING) && GlobalValues.deviceSupportedAbis.none { it.startsWith("arm") } && abi != X86_64) {
                            abi = X86
                        }
                    }
                }
            }

            return if (abi == NO_LIBS && !isApk) {
                getAbiByNativeDir(nativePath)
            } else {
                abi
            }
        } catch (e: Exception) {
            loge(e.toString())
            return ERROR
        } finally {
            zipFile.close()
        }
    }

    /**
     * Get ABI type of an app from native path
     * @param nativePath Native path of the app
     * @return ABI type
     */
    private fun getAbiByNativeDir(nativePath: String): Int {
        val file = File(nativePath.substring(0, nativePath.lastIndexOf("/")))
        val abis = mutableSetOf<Int>()

        val fileList = file.listFiles() ?: return NO_LIBS

        fileList.asSequence()
            .forEach {
                when {
                    it.name.contains("arm64") -> abis.add(ARMV8)
                    it.name.contains("arm") -> abis.add(ARMV7)
                    it.name.contains("x86_64") -> abis.add(X86_64)
                    it.name.contains("x86") -> abis.add(X86)
                    else -> return NO_LIBS
                }
            }

        if (abis.contains(ARMV8)) {
            if (GlobalValues.deviceSupportedAbis.contains(ARMV8_STRING)) {
                return ARMV8
            }
        } else if (abis.contains(ARMV7)) {
            if (GlobalValues.deviceSupportedAbis.contains(ARMV7_STRING)) {
                return ARMV8
            }
        } else if (abis.contains(X86_64)) {
            if (GlobalValues.deviceSupportedAbis.contains(X86_64_STRING) && GlobalValues.deviceSupportedAbis.none { it.startsWith("arm") }) {
                return X86_64
            }
        } else if (abis.contains(X86)) {
            if (GlobalValues.deviceSupportedAbis.contains(X86_STRING) && GlobalValues.deviceSupportedAbis.none { it.startsWith("arm") }) {
                return X86
            }
        }
        return NO_LIBS
    }

    private val ABI_STRING_MAP = hashMapOf(
        ARMV8 to ARMV8_STRING,
        ARMV7 to ARMV7_STRING,
        ARMV5 to ARMV5_STRING,
        X86_64 to X86_64_STRING,
        X86 to X86_STRING,
        NO_LIBS to Utils.getApp().getString(R.string.no_libs),
        ERROR to Utils.getApp().getString(R.string.cannot_read)
    )

    /**
     * Get ABI string from ABI type
     * @param abi ABI type
     * @return ABI string
     */
    fun getAbiString(abi: Int): String {
        return ABI_STRING_MAP[abi] ?: Utils.getApp().getString(R.string.unknown)
    }

    private val ABI_BADGE_MAP = hashMapOf(
        ARMV8 to R.drawable.ic_64bit,
        X86_64 to R.drawable.ic_64bit,
        ARMV7 to R.drawable.ic_32bit,
        ARMV5 to R.drawable.ic_32bit,
        X86 to R.drawable.ic_32bit
    )

    /**
     * Get ABI badge resource from ABI type
     * @param type ABI type
     * @return Badge resource
     */
    fun getAbiBadgeResource(type: Int): Int {
        return ABI_BADGE_MAP[type] ?: R.drawable.ic_no_libs
    }

    /**
     * Format size number to string
     * @param item LibStringItem
     * @return String of size number (100KB)
     */
    fun sizeToString(item: LibStringItem): String {
        val source = item.source?.let { ", ${item.source}" } ?: ""
        return "(${Formatter.formatFileSize(Utils.getApp(), item.size)}$source)"
    }

    /**
     * Get part of DEX classes of an app
     * @param packageName Package name
     * @param isApk True if it is an apk file
     * @return List of LibStringItem
     */
    fun getDexList(packageName: String, isApk: Boolean = false): List<LibStringItem> {

        try {
            val path = if (isApk) {
                packageName
            } else {
                getPackageInfo(packageName).applicationInfo.sourceDir
            }

            if (path.isNullOrEmpty()) {
                return emptyList()
            }
            val apkFile = ApkFile(File(path))
            var splits: List<String>

            val primaryList = apkFile.dexClasses
                .map { it.packageName }
                .filter { !it.startsWith(packageName) }
                .map { item ->
                    splits = item.split(".")
                    when {
                        //Remove obfuscated classes
                        splits.any { it.length == 1 } -> LibStringItem("")
                        //Merge AndroidX classes
                        splits[0] == "androidx" -> LibStringItem("${splits[0]}.${splits[1]}")
                        //Filter classes which paths deep level greater than 4
                        else -> LibStringItem(splits.subList(0, splits.size.coerceAtMost(4)).joinToString(separator = "."))
                    }
                }
                .toSet()
                .filter {
                    it.name.length > 11 && it.name.contains(".") &&
                            (!it.name.contains("0") || !it.name.contains("O") || !it.name.contains("o"))
                }    //Remove obfuscated classes
                .toMutableList()

            //Merge path deep level 3 classes
            primaryList.filter { it.name.split(".").size == 3 }.forEach {
                primaryList.removeAll { item -> item.name.startsWith(it.name) }
                primaryList.add(it)
            }
            //Merge path deep level 4 classes
            var pathLevel3Item: String
            var filter: List<LibStringItem>
            primaryList.filter { it.name.split(".").size == 4 }.forEach {
                if (DexLibMap.DEEP_LEVEL_3_SET.contains(it.name)) {
                    return@forEach
                }

                pathLevel3Item = it.name.split(".").subList(0, 3).joinToString(separator = ".")
                filter = primaryList.filter { item -> item.name.startsWith(pathLevel3Item) }

                if (filter.isNotEmpty()) {
                    primaryList.removeAll(filter)
                    primaryList.add(LibStringItem(pathLevel3Item))
                }
            }
            return primaryList
        } catch (e: Exception) {
            loge(e.toString())
            return emptyList()
        }
    }

    /**
     * Get permissions of an application
     * @param packageName Package name of the app
     * @return Permissions list
     */
    fun getPermissionsList(packageName: String): List<String> {
        return PermissionUtils.getPermissions(packageName)
    }

    fun isIntelCpu(): Boolean {
        return try {
            BufferedReader(FileReader("/proc/cpuinfo"))
                .readLine().contains("Intel")
        } catch (e: Exception) {
            false
        }
    }
}