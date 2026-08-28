package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.reference.TRACE_REFERENCE_BUILD_INDEX
import com.absinthe.libchecker.domain.statistics.reference.TRACE_REFERENCE_LOAD_BATCH
import com.absinthe.libchecker.domain.statistics.reference.TRACE_REFERENCE_MATCH_RULES
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItem
import com.absinthe.libchecker.domain.statistics.reference.traceReferenceComputeTypeName
import com.absinthe.libchecker.domain.statistics.reference.traceReferenceSection
import com.absinthe.libchecker.domain.statistics.reference.traceReferenceSuspendSection
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class ComputeLibReferenceUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend fun buildIndex(
    config: ReferenceConfig,
    onProgress: (Int) -> Unit
  ): ReferenceIndex? = traceReferenceSuspendSection(TRACE_REFERENCE_BUILD_INDEX) {
    val targets = installedAppRepository.getApplicationList()
    val packageInfoByName = targets.associateByTo(HashMap(targets.size)) { it.packageName }
    val index = ReferenceIndex(packageInfoByName)
    val types = getSelectedLibReferenceTypes(config.options)
    val basePackageInfoCache = HashMap<String, PackageInfo>()
    val progressTotal = (targets.size * types.size).coerceAtLeast(1)
    var progressCount = 0

    fun updateProgress(count: Int = progressCount, allowComplete: Boolean = true) {
      onProgress(toProgressPercent(count, progressTotal, allowComplete))
    }

    fun getBasePackageInfo(packageName: String): PackageInfo? {
      basePackageInfoCache[packageName]?.let { return it }
      return packageInfoByName[packageName]
        ?: installedAppRepository.getPackageInfo(packageName)
          ?.also { basePackageInfoCache[packageName] = it }
    }

    fun getPackageInfoFlags(@LibType type: Int): Int? {
      return when (type) {
        SERVICE -> PackageManager.GET_SERVICES
        ACTIVITY -> PackageManager.GET_ACTIVITIES
        RECEIVER -> PackageManager.GET_RECEIVERS
        PROVIDER -> PackageManager.GET_PROVIDERS
        PERMISSION -> PackageManager.GET_PERMISSIONS
        METADATA -> PackageManager.GET_META_DATA
        else -> null
      }
    }

    onProgress(0)

    val batchPackageInfoByType = types.mapNotNull { type ->
      val flags = getPackageInfoFlags(type) ?: return@mapNotNull null
      type to traceReferenceSection(TRACE_REFERENCE_LOAD_BATCH) {
        val packages = installedAppRepository.getInstalledPackages(flags)
        packages.associateByTo(HashMap(packages.size)) { it.packageName }
      }
    }.toMap()

    fun createPackageInfoResolver(@LibType type: Int): (String) -> PackageInfo? {
      val flags = getPackageInfoFlags(type) ?: return ::getBasePackageInfo
      val batchByPackageName = batchPackageInfoByType[type].orEmpty()
      val fallbackCache = HashMap<String, PackageInfo?>()
      return { packageName ->
        batchByPackageName[packageName] ?: if (fallbackCache.containsKey(packageName)) {
          fallbackCache[packageName]
        } else {
          installedAppRepository.getPackageInfo(packageName, flags).also {
            fallbackCache[packageName] = it
          }
        }
      }
    }

    suspend fun computeInternal(@LibType type: Int): Boolean {
      val getPackageInfo = createPackageInfoResolver(type)
      for (target in targets) {
        if (!currentCoroutineContext().isActive) {
          return false
        }

        val applicationInfo = target.applicationInfo
        if (applicationInfo == null) {
          progressCount++
          updateProgress()
          continue
        }
        if (!config.showSystemApps && (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0) {
          progressCount++
          updateProgress()
          continue
        }

        updateProgress(progressCount + 1, allowComplete = false)
        computeComponentReference(index, target.packageName, type, getPackageInfo)
        progressCount++
        updateProgress()
      }
      return true
    }

    for (type in types) {
      val completed = traceReferenceSuspendSection(traceReferenceComputeTypeName(type)) {
        computeInternal(type)
      }
      if (!completed) {
        return@traceReferenceSuspendSection null
      }
    }

    index
  }

  @SuppressLint("WrongConstant")
  suspend fun matchRules(
    index: ReferenceIndex,
    config: MatchConfig,
    onProgress: (Int) -> Unit
  ): List<LibReferenceItem>? = traceReferenceSuspendSection(TRACE_REFERENCE_MATCH_RULES) {
    val references = index.snapshotReferences()
    val refList = mutableListOf<LibReferenceItem>()
    var progressCount = 0

    fun updateProgress(count: Int = progressCount, allowComplete: Boolean = true) {
      val size = references.size
      if (size > 0) {
        onProgress(toProgressPercent(count, size, allowComplete))
      }
    }

    updateProgress()

    for (entry in references) {
      if (!currentCoroutineContext().isActive) {
        return@traceReferenceSuspendSection null
      }

      updateProgress(progressCount + 1, allowComplete = false)
      val libName = entry.name
      val referredList = entry.packageNames
      val type = entry.type
      if (referredList.size >= config.threshold && libName.isNotBlank()) {
        val ruleType = if (type == ACTION) ACTION_IN_RULES else type
        val rule = if (type != PERMISSION && type != METADATA) {
          RulesRepository.getRule(libName, ruleType, true)
        } else {
          null
        }

        if (!config.onlyNotMarked || rule == null) {
          refList.add(
            LibReferenceItem(
              libName,
              rule,
              referredList,
              type
            )
          )
        }
      }

      progressCount++
      updateProgress()
    }

    refList.sortedByDescending { it.referredList.size }
  }

  private fun getSelectedLibReferenceTypes(options: Int): List<Int> {
    return mutableListOf<Int>().apply {
      if (options and LibReferenceOptions.NATIVE_LIBS > 0) add(NATIVE)
      if (options and LibReferenceOptions.SERVICES > 0) add(SERVICE)
      if (options and LibReferenceOptions.ACTIVITIES > 0) add(ACTIVITY)
      if (options and LibReferenceOptions.RECEIVERS > 0) add(RECEIVER)
      if (options and LibReferenceOptions.PROVIDERS > 0) add(PROVIDER)
      if (options and LibReferenceOptions.PERMISSIONS > 0) add(PERMISSION)
      if (options and LibReferenceOptions.METADATA > 0) add(METADATA)
      if (options and LibReferenceOptions.PACKAGES > 0) add(PACKAGE)
      if (options and LibReferenceOptions.SHARED_UID > 0) add(SHARED_UID)
      if (options and LibReferenceOptions.ACTION > 0) add(ACTION)
    }
  }

  private fun toProgressPercent(count: Int, total: Int, allowComplete: Boolean): Int {
    if (count <= 0) {
      return 0
    }
    if (count >= total) {
      return if (allowComplete) 100 else 99
    }
    return (((count * 100) + total - 1) / total).coerceAtMost(99)
  }

  private fun computeComponentReference(
    index: ReferenceIndex,
    packageName: String,
    @LibType type: Int,
    getPackageInfo: (String) -> PackageInfo?
  ) {
    try {
      when (type) {
        NATIVE -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          val list = PackageUtils.getNativeDirLibs(packageInfo)
          val nativeLibNames = list.map { it.name }
          val validationResults = RulesRepository.checkNativeLibValidations(
            packageName = packageName,
            nativeLibs = nativeLibNames,
            otherNativeLibNames = nativeLibNames
          )
          val mapped =
            list.asSequence()
              .filter { validationResults[it.name] == true }
              .map { it.name }
          computeReferenceInternal(
            index,
            packageName,
            NATIVE,
            mapped
          )
        }

        SERVICE -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.services)
        }

        ACTIVITY -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.activities)
        }

        RECEIVER -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.receivers)
        }

        PROVIDER -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.providers)
        }

        DEX -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          val list = PackageUtils.getDexList(packageInfo)
            .asSequence()
            .filter { it.name.startsWith(packageName).not() }
            .map { it.name }
          computeReferenceInternal(
            index,
            packageName,
            DEX,
            list
          )
        }

        PERMISSION -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          computeReferenceInternal(
            index,
            packageName,
            PERMISSION,
            packageInfo.requestedPermissions?.asSequence()
          )
        }

        METADATA -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          computeReferenceInternal(
            index,
            packageName,
            METADATA,
            packageInfo.applicationInfo?.metaData?.keySet()?.asSequence()
          )
        }

        PACKAGE -> {
          val split = packageName.split(".")
          val packagePrefix = split.subList(0, split.size.coerceAtMost(2)).joinToString(".")
          index.addReference(packagePrefix, packageName, PACKAGE)
        }

        SHARED_UID -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          if (packageInfo.sharedUserId?.isNotBlank() == true) {
            index.addReference(packageInfo.sharedUserId!!, packageName, SHARED_UID)
          }
        }

        ACTION -> {
          val packageInfo = getPackageInfo(packageName) ?: return
          val list =
            IntentFilterUtils.parseComponentsFromApk(packageInfo.applicationInfo!!.sourceDir)
              .asSequence()
              .flatMap { component ->
                component.intentFilters.asSequence()
                  .flatMap { filter -> filter.actions }
              }
          // .filter { !it.startsWith("android.") }
          computeReferenceInternal(
            index,
            packageName,
            ACTION,
            list
          )
        }

        else -> {}
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  private fun computeComponentReferenceInternal(
    index: ReferenceIndex,
    packageName: String,
    @LibType type: Int,
    components: Array<out ComponentInfo>?
  ) {
    computeReferenceInternal(
      index,
      packageName,
      type,
      components.orEmpty()
        .asSequence()
        .filter { it.name.startsWith(packageName).not() }
        .map { it.name }
    )
  }

  private fun computeReferenceInternal(
    index: ReferenceIndex,
    packageName: String,
    @LibType type: Int,
    list: Sequence<String>?
  ) {
    list?.forEach {
      index.addReference(it, packageName, type)
    }
  }

  data class ReferenceConfig(
    val showSystemApps: Boolean,
    val options: Int
  )

  data class MatchConfig(
    val threshold: Int,
    val onlyNotMarked: Boolean
  )

  class ReferenceIndex internal constructor(
    internal val packageInfoByName: Map<String, PackageInfo>
  ) {
    private val references = HashMap<String, ReferenceBucket>()

    internal fun addReference(reference: String, packageName: String, @LibType type: Int) {
      synchronized(references) {
        references.getOrPut(reference) { ReferenceBucket(HashSet(), type) }.packageNames.add(packageName)
      }
    }

    internal fun snapshotReferences(): List<ReferenceEntry> {
      return synchronized(references) {
        references.map { (name, bucket) ->
          ReferenceEntry(name, bucket.packageNames, bucket.type)
        }
      }
    }

    fun clear() {
      synchronized(references) {
        references.clear()
      }
    }

    private data class ReferenceBucket(
      val packageNames: MutableSet<String>,
      @LibType val type: Int
    )

    internal data class ReferenceEntry(
      val name: String,
      val packageNames: Set<String>,
      @LibType val type: Int
    )
  }
}
