package com.absinthe.libchecker.domain.app

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager

class GetAlternativeLaunchItemsUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageName: String): List<AlternativeLaunchItem> {
    val activities = installedAppRepository.getPackageInfo(
      packageName = packageName,
      flags = PackageManager.GET_ACTIVITIES
    )?.activities ?: return emptyList()

    return activities.asSequence()
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
