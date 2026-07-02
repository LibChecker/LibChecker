package com.absinthe.libchecker.database

import android.content.Context
import android.database.sqlite.SQLiteException
import android.os.SystemClock
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.rulesbundle.LCRemoteRepo
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import java.io.File
import java.util.Properties
import rikka.core.os.FileUtils as RikkaFileUtils
import timber.log.Timber

object RulesRepository {

  private const val MISSING_RULES_TABLE_MESSAGE = "no such table: rules_table"
  private const val RECOVERY_THROTTLE_MS = 2_000L
  private const val LEGACY_RULES_DATABASE_NAME = "lcrules_database"
  private const val LOCAL_RULES_VERSION_FILE = "lcrules/version"
  private const val RULE_STORE_DIRECTORY_NAME = "lcrules"
  private const val RULE_STORE_FILE_EXTENSION = ".db"
  private const val RULE_STORE_FILE_PREFIX = "rules-v"
  private const val RULES_DB_FILE_NAME = "rules.db"
  private const val RULES_VERSION_ASSET_PATH = "lcrules/version.prop"
  private const val RULES_VERSION_PROPERTY = "version"

  private val recoveryLock = Any()
  private var lastRecoveryUptime = 0L

  fun init(context: Context) {
    migrateLegacyDatabaseIfNeeded(context)
    LCRules.init(context)
    setRemoteRepo(GlobalValues.repo)
  }

  fun reinitialize() {
    init(LibCheckerApp.app)
  }

  fun setRemoteRepo(repo: String) {
    LCRules.setRemoteRepo(
      if (repo == Constants.REPO_GITHUB) {
        LCRemoteRepo.GitHub
      } else {
        LCRemoteRepo.GitLab
      }
    )
  }

  fun getLocalVersion(context: Context): Int {
    return getStoredLocalVersion(context) ?: getBundledVersion(context)
  }

  fun setLocalVersion(context: Context, version: Int) {
    val localVersionDir = File(context.filesDir, LOCAL_RULES_VERSION_FILE)
    if (!localVersionDir.isDirectory) {
      localVersionDir.delete()
    }
    if (!localVersionDir.exists()) {
      localVersionDir.mkdirs()
    }
    File(localVersionDir, version.toString()).createNewFile()
  }

  fun getDatabaseFile(context: Context = LibCheckerApp.app): File {
    return File(getRuleStoreDirectory(context), "$RULE_STORE_FILE_PREFIX${getBundledVersion(context)}$RULE_STORE_FILE_EXTENSION")
  }

  fun getDownloadFile(context: Context): File {
    return File(context.cacheDir, RULES_DB_FILE_NAME)
  }

  fun replaceDatabase(source: File, context: Context = LibCheckerApp.app): Boolean {
    LCRules.close()
    deleteDatabase(context)
    val target = getDatabaseFile(context)
    target.parentFile?.mkdirs()
    RikkaFileUtils.copy(source, target)
    return target.md5() == source.md5()
  }

  fun deleteDatabase(context: Context = LibCheckerApp.app) {
    deleteDatabaseFiles(getDatabaseFile(context))
    deleteDatabaseFiles(getLegacyDatabaseFile(context))
  }

  fun isMissingRulesTableStack(stack: String): Boolean {
    return stack.contains(MISSING_RULES_TABLE_MESSAGE)
  }

  suspend fun getRule(name: String, @LibType type: Int, regex: Boolean): Rule? {
    return try {
      LCRules.getRule(name, type, regex)
    } catch (e: SQLiteException) {
      if (!e.isMissingRulesTable()) {
        throw e
      }
      recover(e)
      runCatching {
        LCRules.getRule(name, type, regex)
      }.onFailure {
        Timber.e(it)
      }.getOrNull()
    }
  }

  suspend fun getRuleWithRegex(
    name: String,
    @LibType type: Int,
    packageName: String? = null,
    nativeLibNames: Collection<String>? = null
  ): Rule? {
    val ruleEntity = getRule(name, type, true) ?: return null
    if (type != NATIVE || packageName == null) {
      return ruleEntity
    }

    if (!requiresNativeLibValidation(name)) {
      return ruleEntity
    }

    if (hasCompanionNativeLibValidation(name, nativeLibNames)) {
      return ruleEntity
    }

    val source = getPackageSourceFile(packageName) ?: return ruleEntity

    if (!checkNativeLibValidation(source, name, nativeLibNames)) {
      return null
    }
    return ruleEntity
  }

  private val NATIVE_SET_QIHOO = setOf("libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so")
  private val NATIVE_SET_SECNEO = setOf("libDexHelper.so", "libDexHelper-x86.so", "libdexjni.so")
  private val NATIVE_SET_FLUTTER = setOf("libapp.so")
  private val NATIVE_SET_UNITY = setOf("libmain.so")
  private val NATIVE_ALL = NATIVE_SET_QIHOO + NATIVE_SET_SECNEO + NATIVE_SET_FLUTTER + NATIVE_SET_UNITY

  private fun requiresNativeLibValidation(nativeLib: String): Boolean {
    return nativeLib in NATIVE_ALL
  }

  private fun hasCompanionNativeLibValidation(
    nativeLib: String,
    otherNativeLibNames: Collection<String>?
  ): Boolean {
    return when {
      nativeLib in NATIVE_SET_FLUTTER -> otherNativeLibNames?.contains("libflutter.so") == true
      nativeLib in NATIVE_SET_UNITY -> otherNativeLibNames?.contains("libunity.so") == true
      else -> false
    }
  }

  private fun getPackageSourceFile(packageName: String): File? {
    return if (packageName.isTempApk()) {
      File(packageName)
    } else {
      runCatching {
        File(PackageUtils.getPackageInfo(packageName).applicationInfo!!.sourceDir)
      }.getOrNull()
    }
  }

  fun checkNativeLibValidation(
    packageName: String,
    nativeLib: String,
    otherNativeLibNames: Collection<String>? = null
  ): Boolean {
    if (!requiresNativeLibValidation(nativeLib)) return true
    val source = getPackageSourceFile(packageName) ?: return false
    return checkNativeLibValidation(source, nativeLib, otherNativeLibNames)
  }

  private fun checkNativeLibValidation(
    source: File,
    nativeLib: String,
    otherNativeLibNames: Collection<String>? = null
  ): Boolean {
    return when {
      NATIVE_SET_QIHOO.contains(nativeLib) -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.qihoo.util.*".toClassDefType(),
              "com.tianyu.util.*".toClassDefType()
            ),
            hasAny = true
          ).isNotEmpty()
        }.getOrDefault(false)
      }

      NATIVE_SET_SECNEO.contains(nativeLib) -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.secneo.apkwrapper.*".toClassDefType()
            )
          ).isNotEmpty()
        }.getOrDefault(false)
      }

      NATIVE_SET_FLUTTER.contains(nativeLib) -> {
        runCatching {
          otherNativeLibNames?.contains("libflutter.so") == true ||
            PackageUtils.findDexClasses(
              source,
              listOf(
                "io.flutter.FlutterInjector".toClassDefType()
              )
            ).isNotEmpty()
        }.getOrDefault(false)
      }

      NATIVE_SET_UNITY.contains(nativeLib) -> {
        runCatching {
          otherNativeLibNames?.contains("libunity.so") == true
        }.getOrDefault(false)
      }

      else -> true
    }
  }

  private fun SQLiteException.isMissingRulesTable(): Boolean {
    return message?.contains(MISSING_RULES_TABLE_MESSAGE, ignoreCase = true) == true
  }

  private fun recover(cause: SQLiteException) {
    synchronized(recoveryLock) {
      val now = SystemClock.elapsedRealtime()
      if (now - lastRecoveryUptime < RECOVERY_THROTTLE_MS) {
        return
      }
      lastRecoveryUptime = now

      Timber.w(cause, "Rules database is missing rules_table, rebuilding.")
      deleteDatabase()
      reinitialize()
    }
  }

  private fun migrateLegacyDatabaseIfNeeded(context: Context) {
    val source = getLegacyDatabaseFile(context)
    val target = getDatabaseFile(context)
    val localVersion = getStoredLocalVersion(context) ?: return
    if (localVersion < getBundledVersion(context) || target.exists() || !source.exists() || source.length() == 0L) {
      return
    }

    runCatching {
      target.parentFile?.mkdirs()
      RikkaFileUtils.copy(source, target)
    }.onFailure {
      FileUtils.delete(target)
      Timber.w(it, "Failed to migrate legacy rules database.")
    }
  }

  private fun getLegacyDatabaseFile(context: Context): File {
    return context.getDatabasePath(LEGACY_RULES_DATABASE_NAME)
  }

  private fun getRuleStoreDirectory(context: Context): File {
    return File(context.noBackupFilesDir, RULE_STORE_DIRECTORY_NAME)
  }

  private fun getStoredLocalVersion(context: Context): Int? {
    val localVersionDir = File(context.filesDir, LOCAL_RULES_VERSION_FILE)
    if (!localVersionDir.isDirectory) {
      return null
    }
    return localVersionDir.listFiles()
      ?.mapNotNull { it.name.toIntOrNull() }
      ?.maxOrNull()
  }

  private fun getBundledVersion(context: Context): Int {
    LCRules.getVersion().takeIf { it > 0 }?.let { return it }
    return readBundledVersion(context)
  }

  private fun readBundledVersion(context: Context): Int {
    return runCatching {
      context.assets.open(RULES_VERSION_ASSET_PATH).use {
        Properties().apply { load(it) }
      }.getProperty(RULES_VERSION_PROPERTY)?.toIntOrNull()
    }.getOrNull() ?: 0
  }

  private fun deleteDatabaseFiles(databaseFile: File) {
    FileUtils.delete(databaseFile)
    FileUtils.delete(File("${databaseFile.path}-shm"))
    FileUtils.delete(File("${databaseFile.path}-wal"))
  }
}
