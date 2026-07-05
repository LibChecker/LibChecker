package com.absinthe.libchecker.domain.app.list.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.AppListItemFactory
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libraries.utils.manager.TimeRecorder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class InitializeAppListUseCase(
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val appListItemFactory: AppListItemFactory
) {

  suspend operator fun invoke(onProgress: (Int) -> Unit): Boolean {
    Timber.d("initItems: START")
    val timeRecorder = TimeRecorder()
    timeRecorder.start()

    val appList = installedAppRepository.getApplicationList(true)
    val progressTotal = appList.size.coerceAtLeast(1)
    val lcItems = mutableListOf<LCItem>()
    var progressCount = 0

    appListRepository.clearItems()
    onProgress(0)

    for (info in appList) {
      try {
        lcItems.add(appListItemFactory.create(info, delayInitFeatures = true))
        progressCount++
        onProgress(progressCount * 100 / progressTotal)
      } catch (e: Throwable) {
        Timber.e(e, "initItems: ${info.packageName}")
        continue
      }

      if (lcItems.size == BATCH_SIZE) {
        appListRepository.insertItems(lcItems)
        lcItems.clear()
      }

      if (!currentCoroutineContext().isActive) {
        return false
      }
    }

    if (lcItems.isNotEmpty()) {
      appListRepository.insertItems(lcItems)
    }

    timeRecorder.end()
    Timber.d("initItems: END, $timeRecorder")
    return true
  }

  private companion object {
    const val BATCH_SIZE = 50
  }
}
