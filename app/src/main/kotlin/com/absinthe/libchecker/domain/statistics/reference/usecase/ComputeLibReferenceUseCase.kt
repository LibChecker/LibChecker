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
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItem
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
  ): ReferenceIndex? {
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

    suspend fun computeInternal(@LibType type: Int): Boolean {
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
        computeComponentReference(index, target.packageName, type, ::getBasePackageInfo)
        progressCount++
        updateProgress()
      }
      return true
    }

    onProgress(0)

    for (type in types) {
      if (!computeInternal(type)) {
        return null
      }
    }

    return index
  }

  @SuppressLint("WrongConstant")
  suspend fun matchRules(
    index: ReferenceIndex,
    config: MatchConfig,
    onProgress: (Int) -> Unit
  ): List<LibReferenceItem>? {
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
        return null
      }

      updateProgress(progressCount + 1, allowComplete = false)
      val libName = entry.key
      val referredList = entry.value.first
      val type = entry.value.second
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
              referredList.toSet(),
              type
            )
          )
        }
      }

      progressCount++
      updateProgress()
    }

    return refList.sortedByDescending { it.referredList.size }
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
    getBasePackageInfo: (String) -> PackageInfo?
  ) {
    try {
      when (type) {
        NATIVE -> {
          val packageInfo = getBasePackageInfo(packageName) ?: return
          val list = PackageUtils.getNativeDirLibs(packageInfo)
          val nativeLibNames = list.map { it.name }
          val mapped =
            list.asSequence()
              .filter { RulesRepository.checkNativeLibValidation(packageName, it.name, nativeLibNames) }
              .map { it.name }
          computeReferenceInternal(
            index,
            packageName,
            NATIVE,
            mapped
          )
        }

        SERVICE -> {
          val packageInfo = installedAppRepository.getPackageInfo(
            packageName,
            PackageManager.GET_SERVICES
          ) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.services)
        }

        ACTIVITY -> {
          val packageInfo = installedAppRepository.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES
          ) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.activities)
        }

        RECEIVER -> {
          val packageInfo = installedAppRepository.getPackageInfo(
            packageName,
            PackageManager.GET_RECEIVERS
          ) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.receivers)
        }

        PROVIDER -> {
          val packageInfo = installedAppRepository.getPackageInfo(
            packageName,
            PackageManager.GET_PROVIDERS
          ) ?: return
          computeComponentReferenceInternal(index, packageName, type, packageInfo.providers)
        }

        DEX -> {
          val packageInfo = getBasePackageInfo(packageName) ?: return
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
          val packageInfo = installedAppRepository.getPackageInfo(
            packageName,
            PackageManager.GET_PERMISSIONS
          ) ?: return
          computeReferenceInternal(
            index,
            packageName,
            PERMISSION,
            packageInfo.requestedPermissions?.asSequence()
          )
        }

        METADATA -> {
          val packageInfo = installedAppRepository.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
          ) ?: return
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
          val packageInfo = getBasePackageInfo(packageName) ?: return
          if (packageInfo.sharedUserId?.isNotBlank() == true) {
            index.addReference(packageInfo.sharedUserId!!, packageName, SHARED_UID)
          }
        }

        ACTION -> {
          val packageInfo = getBasePackageInfo(packageName) ?: return
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
    private val references = HashMap<String, Pair<MutableSet<String>, Int>>()

    internal fun addReference(reference: String, packageName: String, @LibType type: Int) {
      synchronized(references) {
        references.getOrPut(reference) { HashSet<String>() to type }.first.add(packageName)
      }
    }

    internal fun snapshotReferences(): Map<String, Pair<Set<String>, Int>> {
      return synchronized(references) {
        references.mapValues { (_, value) -> value.first.toSet() to value.second }
      }
    }

    fun clear() {
      synchronized(references) {
        references.clear()
      }
    }
  }
}
