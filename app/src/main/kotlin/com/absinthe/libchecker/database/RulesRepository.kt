package com.absinthe.libchecker.database

import android.content.Context
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.os.SystemClock
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.data.rules.RuleBundleStore
import com.absinthe.libchecker.domain.rules.RuleBundleManifest
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.rulesbundle.LCRemoteRepo
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object RulesRepository {

  private const val MISSING_RULES_TABLE_MESSAGE = "no such table: rules_table"
  private const val RECOVERY_THROTTLE_MS = 2_000L
  private val recoveryLock = Any()
  private var lastRecoveryUptime = 0L

  @Volatile private var initialized = false

  @Volatile private var activeBundle: RuleBundleStore.Installed? = null

  fun bundleStore(context: Context) = RuleBundleStore(File(context.noBackupFilesDir, "rules-v5"))

  fun init(context: Context) = synchronized(recoveryLock) initialization@{
    if (initialized) return@initialization
    synchronized(LCRules) {
      LCRules.init(context)
      val baselineVersion = LCRules.getMetadata().dataVersion
      val store = bundleStore(context)
      activeBundle = null
      val candidates = store.candidates().filter { it.manifest.dataVersion > baselineVersion }
        .sortedByDescending { it.manifest.dataVersion }.filter { candidate ->
          runCatching {
            runCatching { store.validateInstalled(candidate) }.getOrElse { _ ->
              store.install(candidate.manifest, File(candidate.directory, "archive.zip"), activate = false, repairExisting = true)
            }
          }.onFailure { Timber.w(it, "Cannot open downloaded rules; retaining previous/bundled data") }.isSuccess
        }
      runCatching { store.pruneUnreferenced(candidates.map { it.directory }.toSet(), baselineVersion.toInt()) }
        .onFailure { Timber.w(it, "Cannot prune unused rules") }
      candidates.firstOrNull { candidate ->
        runCatching { LCRules.activateDatabase(candidate.database) }
          .onSuccess { activeBundle = candidate }
          .onFailure { Timber.w(it, "Cannot activate downloaded rules; trying previous/bundled data") }.isSuccess
      }
      runCatching { pruneLegacyRules(context.noBackupFilesDir, context.filesDir, context.getDatabasePath("lcrules_database")) }
        .onFailure { Timber.w(it, "Cannot prune unused legacy rules") }
      setRemoteRepo(GlobalValues.repo)
      initialized = true
    }
  }

  fun reinitialize() = synchronized(recoveryLock) {
    initialized = false
  }

  fun installBundle(context: Context, manifest: RuleBundleManifest, archive: File): Boolean = synchronized(recoveryLock) {
    runCatching { bundleStore(context).install(manifest, archive) }.onFailure { Timber.w(it) }.isSuccess
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
    init(context)
    return LCRules.getMetadata().dataVersion.toInt()
  }

  fun getDatabaseFile(context: Context = LibCheckerApp.app): File {
    init(context)
    return LCRules.getDatabaseFile()
  }

  fun getDownloadFile(context: Context): File = File(context.cacheDir, "rules-v5.zip")

  fun deleteDatabase() = synchronized(recoveryLock) {
    synchronized(LCRules) {
      LCRules.close()
      activeBundle?.let { File(it.directory, "invalid").writeText("invalid") }
      initialized = false
    }
  }

  fun isMissingRulesTableStack(stack: String): Boolean {
    return stack.contains(MISSING_RULES_TABLE_MESSAGE)
  }

  suspend fun getRule(name: String, @LibType type: Int, regex: Boolean): Rule? {
    return withContext(Dispatchers.IO) {
      init(LibCheckerApp.app)
      try {
        LCRules.getRule(name, type, regex, GlobalValues.preferredRuleLanguage)
      } catch (e: SQLiteException) {
        if (!e.isRecoverableRulesDatabaseFailure()) throw e
        recover(e)
        try {
          LCRules.getRule(name, type, regex, GlobalValues.preferredRuleLanguage)
        } catch (retryFailure: SQLiteException) {
          if (!retryFailure.isRecoverableRulesDatabaseFailure()) throw retryFailure
          Timber.e(retryFailure)
          null
        }
      }
    }
  }

  suspend fun getRuleWithRegex(
    name: String,
    @LibType type: Int,
    packageName: String? = null,
    nativeLibNames: Collection<String>? = null
  ): Rule? {
    return getRulesWithRegex(
      names = listOf(name),
      type = type,
      packageName = packageName,
      nativeLibNames = nativeLibNames
    )[name]
  }

  suspend fun getRulesWithRegex(
    names: Collection<String>,
    @LibType type: Int,
    packageName: String? = null,
    nativeLibNames: Collection<String>? = null
  ): Map<String, Rule?> {
    val distinctNames = if (names is Set<String>) names else names.toSet()
    val rules = distinctNames.associateWith { getRule(it, type, true) }
    if (type != NATIVE || packageName == null) {
      return rules
    }

    val namesToValidate = rules
      .filterValues { it != null }
      .keys
      .filter(::requiresNativeLibValidation)
    if (namesToValidate.isEmpty()) {
      return rules
    }

    val source = getPackageSourceFile(packageName) ?: return rules
    val validationResults = checkNativeLibValidations(
      source = source,
      nativeLibs = namesToValidate,
      otherNativeLibNames = nativeLibNames
    )
    return rules.mapValues { (name, rule) ->
      rule?.takeIf { validationResults[name] != false }
    }
  }

  private val NATIVE_SET_QIHOO = setOf("libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so")
  private val NATIVE_SET_SECNEO = setOf("libDexHelper.so", "libDexHelper-x86.so", "libdexjni.so")
  private val NATIVE_SET_FLUTTER = setOf("libapp.so")
  private val NATIVE_SET_UNITY = setOf("libmain.so")
  private val NATIVE_ALL = NATIVE_SET_QIHOO + NATIVE_SET_SECNEO + NATIVE_SET_FLUTTER + NATIVE_SET_UNITY
  private val CLASS_PATTERNS_QIHOO = setOf(
    "com.qihoo.util.*".toClassDefType(),
    "com.tianyu.util.*".toClassDefType()
  )
  private val CLASS_PATTERNS_SECNEO = setOf("com.secneo.apkwrapper.*".toClassDefType())
  private val CLASS_PATTERNS_FLUTTER = setOf("io.flutter.FlutterInjector".toClassDefType())

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

  private fun requiresNativeLibValidationSource(
    nativeLib: String,
    otherNativeLibNames: Collection<String>?
  ): Boolean {
    if (hasCompanionNativeLibValidation(nativeLib, otherNativeLibNames)) {
      return false
    }
    return nativeLib in NATIVE_SET_QIHOO ||
      nativeLib in NATIVE_SET_SECNEO ||
      nativeLib in NATIVE_SET_FLUTTER
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
    return checkNativeLibValidations(
      packageName = packageName,
      nativeLibs = listOf(nativeLib),
      otherNativeLibNames = otherNativeLibNames
    )[nativeLib] == true
  }

  fun checkNativeLibValidations(
    packageName: String,
    nativeLibs: Collection<String>,
    otherNativeLibNames: Collection<String>? = null
  ): Map<String, Boolean> {
    return checkNativeLibValidations(
      nativeLibs = nativeLibs,
      otherNativeLibNames = otherNativeLibNames,
      sourceProvider = { getPackageSourceFile(packageName) }
    )
  }

  internal fun checkNativeLibValidations(
    nativeLibs: Collection<String>,
    otherNativeLibNames: Collection<String>? = null,
    sourceProvider: () -> File?
  ): Map<String, Boolean> {
    val distinctNativeLibs = if (nativeLibs is Set<String>) nativeLibs else nativeLibs.toSet()
    val namesToValidate = distinctNativeLibs.filter(::requiresNativeLibValidation)
    if (namesToValidate.isEmpty()) {
      return distinctNativeLibs.associateWith { true }
    }
    if (namesToValidate.none { requiresNativeLibValidationSource(it, otherNativeLibNames) }) {
      return distinctNativeLibs.associateWith { nativeLib ->
        nativeLib !in namesToValidate || hasCompanionNativeLibValidation(nativeLib, otherNativeLibNames)
      }
    }
    val source = sourceProvider()
      ?: return distinctNativeLibs.associateWith { nativeLib ->
        nativeLib !in namesToValidate || hasCompanionNativeLibValidation(nativeLib, otherNativeLibNames)
      }
    val validationResults = checkNativeLibValidations(
      source = source,
      nativeLibs = namesToValidate,
      otherNativeLibNames = otherNativeLibNames
    )
    return distinctNativeLibs.associateWith { validationResults[it] ?: true }
  }

  private fun checkNativeLibValidations(
    source: File,
    nativeLibs: Collection<String>,
    otherNativeLibNames: Collection<String>? = null
  ): Map<String, Boolean> {
    val patterns = buildSet {
      nativeLibs.forEach { nativeLib ->
        if (!hasCompanionNativeLibValidation(nativeLib, otherNativeLibNames)) {
          when {
            nativeLib in NATIVE_SET_QIHOO -> addAll(CLASS_PATTERNS_QIHOO)
            nativeLib in NATIVE_SET_SECNEO -> addAll(CLASS_PATTERNS_SECNEO)
            nativeLib in NATIVE_SET_FLUTTER -> addAll(CLASS_PATTERNS_FLUTTER)
          }
        }
      }
    }
    val foundClasses = if (patterns.isEmpty()) {
      emptySet()
    } else {
      runCatching {
        PackageUtils.findDexClasses(source, patterns.toList()).toSet()
      }.getOrDefault(emptySet())
    }

    return nativeLibs.associateWith { nativeLib ->
      when {
        hasCompanionNativeLibValidation(nativeLib, otherNativeLibNames) -> true
        nativeLib in NATIVE_SET_QIHOO -> foundClasses.any(CLASS_PATTERNS_QIHOO::contains)
        nativeLib in NATIVE_SET_SECNEO -> foundClasses.any(CLASS_PATTERNS_SECNEO::contains)
        nativeLib in NATIVE_SET_FLUTTER -> foundClasses.any(CLASS_PATTERNS_FLUTTER::contains)
        nativeLib in NATIVE_SET_UNITY -> false
        else -> true
      }
    }
  }

  private fun SQLiteException.isRecoverableRulesDatabaseFailure(): Boolean {
    return this is SQLiteDatabaseCorruptException ||
      message?.contains(MISSING_RULES_TABLE_MESSAGE, ignoreCase = true) == true
  }

  private fun recover(cause: SQLiteException) {
    synchronized(recoveryLock) {
      val now = SystemClock.elapsedRealtime()
      if (now - lastRecoveryUptime < RECOVERY_THROTTLE_MS) {
        return
      }
      lastRecoveryUptime = now

      Timber.w(cause, "Rules database is missing or corrupt, rebuilding.")
      LCRules.close()
      deleteDatabase()
      reinitialize()
      init(LibCheckerApp.app)
    }
  }

  internal fun pruneLegacyRules(noBackup: File, files: File, database: File) {
    check(File(noBackup, "rules-legacy").deleteRecursively())
    File(noBackup, "lcrules").listFiles()?.filter {
      it.name.matches(Regex("rules-v[0-9]+\\.db(?:-wal|-shm|-journal)?"))
    }?.forEach { check(it.delete()) }
    File(files, "lcrules/version").listFiles()?.filter { it.name.toIntOrNull() != null }
      ?.forEach { check(it.delete()) }
    listOf("", "-wal", "-shm", "-journal").map { File(database.path + it) }
      .filter(File::exists).forEach { check(it.delete()) }
  }
}
