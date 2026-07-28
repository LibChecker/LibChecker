package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class OfficialStatisticCatalogDataSource(
  private val remoteSource: OfficialStatisticRemoteSource,
  private val bundleStore: OfficialStatisticBundleStore,
  private val appVersionCode: Long
) : RemoteStatisticCatalogDataSource {

  override suspend fun getCachedStatistics(): List<StatisticDefinition> = withContext(Dispatchers.IO) {
    bundleStore.loadCachedStatistics()
  }

  override suspend fun refreshStatistics(): List<StatisticDefinition>? = withContext(Dispatchers.IO) {
    runCatching {
      val manifest = remoteSource.getManifest()
      bundleStore.validateManifest(manifest)
      if (manifest.minimumAppVersionCode > appVersionCode) {
        return@runCatching bundleStore.loadCachedStatistics()
      }
      if (manifest.bundleSha256 == bundleStore.currentSha256) {
        return@runCatching bundleStore.loadCachedStatistics()
      }
      val destination = bundleStore.downloadFile
      try {
        remoteSource.downloadBundle(destination, manifest.bundleSize)
        bundleStore.install(manifest, destination)
      } finally {
        destination.delete()
      }
    }.onFailure { error ->
      Timber.w(error, "Unable to refresh official chart rules")
    }.getOrNull()
  }
}
