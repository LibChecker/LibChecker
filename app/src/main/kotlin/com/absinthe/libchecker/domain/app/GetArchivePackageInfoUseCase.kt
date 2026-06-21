package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.utils.apk.APKSParser
import com.absinthe.libchecker.utils.apk.XAPKParser
import java.io.File
import timber.log.Timber

class GetArchivePackageInfoUseCase {

  operator fun invoke(file: File): PackageInfo? {
    return getPackageArchiveInfo(file)
      ?: getApksPackageInfo(file)
      ?: getXapkPackageInfo(file)
  }

  private fun getPackageArchiveInfo(file: File): PackageInfo? {
    return PackageManagerCompat.getPackageArchiveInfo(file.path, PACKAGE_INFO_FLAGS)
      ?.also { packageInfo ->
        packageInfo.applicationInfo?.let { applicationInfo ->
          applicationInfo.sourceDir = file.path
          applicationInfo.publicSourceDir = file.path
        }
      }
      ?: run {
        Timber.w("Failed to get PackageArchiveInfo")
        null
      }
  }

  private fun getApksPackageInfo(file: File): PackageInfo? {
    val packageInfo = APKSParser(file, PACKAGE_INFO_FLAGS).getPackageInfo()
    if (packageInfo == null) {
      Timber.w("Not APKS file")
    }
    return packageInfo
  }

  private fun getXapkPackageInfo(file: File): PackageInfo? {
    return XAPKParser(file, PACKAGE_INFO_FLAGS).getPackageInfo()
      ?: run {
        Timber.w("Not XAPK file")
        null
      }
  }

  private companion object {
    private val PACKAGE_INFO_FLAGS = PackageManager.GET_SERVICES or
      PackageManager.GET_ACTIVITIES or
      PackageManager.GET_RECEIVERS or
      PackageManager.GET_PROVIDERS or
      PackageManager.GET_PERMISSIONS or
      PackageManager.GET_META_DATA or
      PackageManager.MATCH_DISABLED_COMPONENTS or
      PackageManager.MATCH_UNINSTALLED_PACKAGES
  }
}
