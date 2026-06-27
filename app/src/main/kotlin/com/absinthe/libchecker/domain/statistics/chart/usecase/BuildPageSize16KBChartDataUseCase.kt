package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.is16KBAligned
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class BuildPageSize16KBChartDataUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    request: Request,
    onProgress: suspend (Int) -> Unit
  ): PageSize16KBChartData? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val support16KB = mutableListOf<LCItem>()
    val notSupport16KB = mutableListOf<LCItem>()
    val noNativeLibs = mutableListOf<LCItem>()
    val coroutineContext = currentCoroutineContext()
    val itemCount = targets.size
    var progress = 0

    targets.forEachIndexed { index, item ->
      if (!coroutineContext.isActive) {
        return null
      }

      runCatching {
        val packageInfo = installedAppRepository.getPackageInfo(item.packageName) ?: return@runCatching
        if (PackageUtils.hasNoNativeLibs(item.abi.toInt())) {
          noNativeLibs.add(item)
        } else if (packageInfo.is16KBAligned()) {
          support16KB.add(item)
        } else {
          notSupport16KB.add(item)
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

    return PageSize16KBChartData(
      support16KB = support16KB,
      notSupport16KB = notSupport16KB,
      noNativeLibs = noNativeLibs
    )
  }

  data class Request(
    val items: List<LCItem>,
    val showSystemApps: Boolean
  )
}

data class PageSize16KBChartData(
  val support16KB: List<LCItem>,
  val notSupport16KB: List<LCItem>,
  val noNativeLibs: List<LCItem>
)
