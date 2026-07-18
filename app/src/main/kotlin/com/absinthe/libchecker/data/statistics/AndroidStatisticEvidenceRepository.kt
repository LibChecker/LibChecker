package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import com.absinthe.libchecker.utils.PackageUtils
import java.io.File
import timber.log.Timber

class AndroidStatisticEvidenceRepository(
  private val installedAppRepository: InstalledAppRepository
) : StatisticEvidenceRepository {

  override fun hasNativeLibrary(packageName: String, libraryName: String): Boolean {
    return runCatching {
      val packageInfo = installedAppRepository.getPackageInfo(packageName)
        ?: return@runCatching false
      val nativeDirectory = packageInfo.applicationInfo?.nativeLibraryDir?.let(::File)
      val extractedLibraries = nativeDirectory?.listFiles()
        ?.filter { it.isFile && it.extension == "so" }
        .orEmpty()
      if (extractedLibraries.isNotEmpty()) {
        extractedLibraries.any { it.name == libraryName }
      } else {
        PackageUtils.getNativeDirLibs(packageInfo, parseElf = false)
          .any { it.name == libraryName }
      }
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(false)
  }
}
