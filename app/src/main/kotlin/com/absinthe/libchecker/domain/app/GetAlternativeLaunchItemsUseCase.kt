package com.absinthe.libchecker.domain.app

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAlternativeLaunchItemsUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String): List<AlternativeLaunchItem> =
    withContext(Dispatchers.IO) {
      val activities = installedAppRepository.getPackageInfo(
        packageName = packageName,
        flags = PackageManager.GET_ACTIVITIES
      )?.activities ?: return@withContext emptyList()

      activities.asSequence()
        .filter(ActivityInfo::exported)
        .map {
          AlternativeLaunchItem(
            label = it.loadLabelOrName(),
            className = it.name
          )
        }
        .toList()
    }

  private fun ActivityInfo.loadLabelOrName(): String {
    return runCatching {
      loadLabel(packageManager).toString()
    }.getOrDefault(name)
  }
}

data class AlternativeLaunchItem(
  val label: String,
  val className: String
)
