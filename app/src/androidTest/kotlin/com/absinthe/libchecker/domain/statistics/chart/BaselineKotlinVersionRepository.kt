package com.absinthe.libchecker.domain.statistics.chart

import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.buildmetadata.BaselineKotlinBuildMetadataDetector
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinVersionInferenceHints
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.KotlinVersionRepository
import java.io.File
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Frozen pre-optimization chart repository; never used by the application. */
internal class BaselineKotlinVersionRepository(
  private val installedAppRepository: InstalledAppRepository
) : KotlinVersionRepository {
  override suspend fun getVersion(packageName: String): String? {
    return try {
      val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return null
      val apk = File(packageInfo.applicationInfo?.sourceDir ?: return null)
      val components = installedAppRepository.getPackageInfo(
        packageName,
        flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
          PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_INSTRUMENTATION,
        resolveFrozenArchiveInfo = false
      )
      ZipFileCompat(apk).use { zip ->
        BaselineKotlinBuildMetadataDetector.detect(
          apk,
          zip,
          inferenceHints = KotlinVersionInferenceHints.from(packageInfo, components)
        ).kotlinVersion
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e)
      null
    }
  }
}
