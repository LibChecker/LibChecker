package com.absinthe.libchecker.domain.app.list.usecase

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.extensions.getFeatures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import timber.log.Timber

class InitializePendingAppFeaturesUseCase(
  private val appListRepository: AppListRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(batchSize: Int = DEFAULT_BATCH_SIZE) {
    require(batchSize > 0)
    val context = currentCoroutineContext()
    val pendingPackages = appListRepository.getUninitializedFeaturePackageNames()
    val featuresMap = HashMap<String, Int>(batchSize)

    suspend fun flushFeatures() {
      context.ensureActive()
      if (featuresMap.isEmpty()) {
        return
      }
      appListRepository.updateFeatures(featuresMap)
      featuresMap.clear()
    }

    pendingPackages.forEach { packageName ->
      context.ensureActive()
      runCatching {
        val packageInfo = installedAppRepository.getPackageInfo(packageName, PackageManager.GET_META_DATA)
          ?: return@runCatching
        featuresMap[packageName] = packageInfo.getFeatures { context.ensureActive() }
        if (featuresMap.size >= batchSize) {
          flushFeatures()
        }
      }.onFailure { e ->
        if (e is CancellationException) throw e
        Timber.w(e)
      }
    }
    flushFeatures()
  }

  private companion object {
    const val DEFAULT_BATCH_SIZE = 32
  }
}
