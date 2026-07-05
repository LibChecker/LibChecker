package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import java.util.TreeMap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class BuildApiLevelChartDataUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(request: Request): Map<Int, List<LCItem>> {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val result = TreeMap<Int, MutableList<LCItem>>()
    val coroutineContext = currentCoroutineContext()

    for (item in targets) {
      if (!coroutineContext.isActive) {
        break
      }
      val apiLevel = runCatching {
        val packageInfo = installedAppRepository.getPackageInfo(item.packageName) ?: return@runCatching null
        when (request.kind) {
          Kind.TargetSdk -> packageInfo.applicationInfo?.targetSdkVersion
          Kind.MinSdk -> packageInfo.applicationInfo?.minSdkVersion
          Kind.CompileSdk -> packageInfo.getCompileSdkVersion()
        }
      }.onFailure {
        Timber.e(it)
      }.getOrNull() ?: continue

      result.getOrPut(apiLevel) { mutableListOf() }.add(item)
    }

    return result
  }

  data class Request(
    val items: List<LCItem>,
    val kind: Kind,
    val showSystemApps: Boolean
  )

  enum class Kind {
    TargetSdk,
    MinSdk,
    CompileSdk
  }
}
