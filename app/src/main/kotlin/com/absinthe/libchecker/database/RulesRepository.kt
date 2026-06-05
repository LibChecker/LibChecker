package com.absinthe.libchecker.database

import android.database.sqlite.SQLiteException
import android.os.SystemClock
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import java.io.File
import timber.log.Timber

object RulesRepository {

  private const val MISSING_RULES_TABLE_MESSAGE = "no such table: rules_table"
  private const val RECOVERY_THROTTLE_MS = 2_000L

  private val recoveryLock = Any()
  private var lastRecoveryUptime = 0L

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

    if (packageName.isTempApk()) {
      File(packageName)
    } else {
      runCatching {
        File(PackageUtils.getPackageInfo(packageName).applicationInfo!!.sourceDir)
      }.getOrNull()
    } ?: return ruleEntity

    if (!checkNativeLibValidation(packageName, name, nativeLibNames)) {
      return null
    }
    return ruleEntity
  }

  private val NATIVE_SET_QIHOO = setOf("libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so")
  private val NATIVE_SET_SECNEO = setOf("libDexHelper.so", "libDexHelper-x86.so", "libdexjni.so")
  private val NATIVE_SET_FLUTTER = setOf("libapp.so")
  private val NATIVE_SET_UNITY = setOf("libmain.so")
  private val NATIVE_ALL = NATIVE_SET_QIHOO + NATIVE_SET_SECNEO + NATIVE_SET_FLUTTER + NATIVE_SET_UNITY

  fun checkNativeLibValidation(
    packageName: String,
    nativeLib: String,
    otherNativeLibNames: Collection<String>? = null
  ): Boolean {
    if (!NATIVE_ALL.contains(nativeLib)) return true
    val sourceDir = PackageUtils.getPackageInfo(packageName).applicationInfo?.sourceDir ?: return false
    val source = File(sourceDir)
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
      LCRules.closeDb()
      Repositories.deleteRulesDatabase()
      LCRules.init(LibCheckerApp.app)
    }
  }
}
