package com.absinthe.libchecker.data.statistics

import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinBuildMetadataDetector
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinVersionInferenceHints
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.KotlinVersionRepository
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class AndroidKotlinVersionRepository(
  private val installedAppRepository: InstalledAppRepository
) : KotlinVersionRepository {
  // Fixed lock stripes coalesce scans for the same package without serializing every app.
  private val scanLocks = Array(16) { Mutex() }
  private val cache = object : LinkedHashMap<CacheKey, CachedVersion>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, CachedVersion>?): Boolean = size > 512
  }

  override suspend fun getVersion(packageName: String): String? = scanLocks[packageName.hashCode() and 15].withLock {
    val coroutineContext = currentCoroutineContext()
    coroutineContext.ensureActive()
    try {
      val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return@withLock null
      val appInfo = packageInfo.applicationInfo ?: return@withLock null
      val apk = File(appInfo.sourceDir ?: return@withLock null)
      if (!apk.isFile) return@withLock null
      val key = CacheKey(packageName, packageInfo.lastUpdateTime, apk.path, apk.length(), apk.lastModified(), appInfo.splitSourceDirs?.toList().orEmpty())
      synchronized(cache) { cache[key] }?.let { return@withLock it.version }
      var readFailed = false
      val version = ZipFileCompat(apk).use { zip ->
        KotlinBuildMetadataDetector.detect(
          apk,
          zip,
          loadInferenceHints = {
            val components = installedAppRepository.getPackageInfo(
              packageName,
              flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_INSTRUMENTATION,
              resolveFrozenArchiveInfo = false
            )
            if (components == null) readFailed = true
            KotlinVersionInferenceHints.from(packageInfo, components)
          },
          checkCancelled = coroutineContext::ensureActive,
          onReadFailure = { readFailed = true }
        ).kotlinVersion
      }
      coroutineContext.ensureActive()
      if (!readFailed) {
        synchronized(cache) {
          cache.keys.removeAll { it.packageName == packageName && it != key }
          cache[key] = CachedVersion(version)
        }
      }
      version
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e)
      null
    }
  }

  private data class CachedVersion(val version: String?)

  private data class CacheKey(
    val packageName: String,
    val lastUpdateTime: Long,
    val sourcePath: String,
    val sourceSize: Long,
    val sourceModifiedTime: Long,
    val splitPaths: List<String>
  )
}
