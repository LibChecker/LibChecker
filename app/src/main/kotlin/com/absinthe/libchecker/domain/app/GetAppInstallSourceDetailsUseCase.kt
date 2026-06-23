package com.absinthe.libchecker.domain.app

import android.content.Context
import android.content.pm.PackageInfo
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.PREINSTALLED_TIMESTAMP
import com.absinthe.libchecker.utils.extensions.getDexFileOptimizationInfo
import java.text.SimpleDateFormat

class GetAppInstallSourceDetailsUseCase(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageName: String): AppInstallSourceDetails? {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return null
    return AppInstallSourceDetails(
      installSource = installedAppRepository.getInstallSource(packageName),
      installedTime = packageInfo.getInstalledTimeDisplayData(
        showInstalledTime = !installedAppRepository.getPackageState(packageName).isFrozen
      ),
      dexoptInfo = packageInfo.getDexFileOptimizationInfo()
    )
  }

  private fun PackageInfo.getInstalledTimeDisplayData(
    showInstalledTime: Boolean
  ): AppInstalledTimeDisplayData? {
    if (!showInstalledTime) {
      return null
    }
    return AppInstalledTimeDisplayData(
      firstInstalledTime = firstInstallTime.toInstalledTimeText(),
      lastUpdatedTime = lastUpdateTime.toInstalledTimeText()
    )
  }

  private fun Long.toInstalledTimeText(): String {
    return if (this <= PREINSTALLED_TIMESTAMP) {
      context.getString(R.string.snapshot_preinstalled_app)
    } else {
      SimpleDateFormat.getDateTimeInstance().format(this)
    }
  }
}
