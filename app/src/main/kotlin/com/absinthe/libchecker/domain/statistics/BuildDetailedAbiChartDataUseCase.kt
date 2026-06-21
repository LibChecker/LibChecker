package com.absinthe.libchecker.domain.statistics

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import java.io.File
import java.util.TreeMap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class BuildDetailedAbiChartDataUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    request: Request,
    onProgress: suspend (Int) -> Unit
  ): Map<Int, List<LCItem>>? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val result = TreeMap<Int, MutableList<LCItem>>()
    val coroutineContext = currentCoroutineContext()
    val itemCount = targets.size
    var progress = 0

    targets.forEachIndexed { index, item ->
      if (!coroutineContext.isActive) {
        return null
      }

      runCatching {
        val packageInfo = installedAppRepository.getPackageInfo(item.packageName) ?: return@runCatching
        val source = packageInfo.applicationInfo?.sourceDir ?: return@runCatching
        PackageUtils.getAbiSet(
          file = File(source),
          packageInfo = packageInfo,
          isApk = false,
          ignoreArch = true
        ).forEach { abi ->
          result.getOrPut(abi) { mutableListOf() }.add(item)
        }
      }.onFailure {
        Timber.e(it)
      }

      if (itemCount > 0) {
        val nextProgress = index * 100 / itemCount
        if (nextProgress > progress) {
          progress = nextProgress
          onProgress(progress)
        }
      }
    }

    return result
  }

  data class Request(
    val items: List<LCItem>,
    val showSystemApps: Boolean
  )
}
