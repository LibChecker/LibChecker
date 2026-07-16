package com.absinthe.libchecker.domain.app.list.usecase

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.extensions.getFeatures
import timber.log.Timber

class InitializePendingAppFeaturesUseCase(
  private val appListRepository: AppListRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(batchSize: Int = DEFAULT_BATCH_SIZE) {
    val pendingPackages = appListRepository.getUninitializedFeaturePackageNames()
    val featuresMap = HashMap<String, Int>(batchSize)

    suspend fun flushFeatures() {
      if (featuresMap.isEmpty()) {
        return
      }
      appListRepository.updateFeatures(featuresMap)
      featuresMap.clear()
    }

    pendingPackages.forEach { packageName ->
      runCatching {
        val packageInfo = installedAppRepository.getPackageInfo(packageName, PackageManager.GET_META_DATA)
          ?: return@runCatching
        featuresMap[packageName] = packageInfo.getFeatures()
        if (featuresMap.size >= batchSize) {
          flushFeatures()
        }
      }.onFailure { e ->
        Timber.w(e)
      }
    }
    flushFeatures()
  }

  private companion object {
    const val DEFAULT_BATCH_SIZE = 32
  }
}
