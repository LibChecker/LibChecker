package com.absinthe.libchecker.domain.app.detail.content

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.getSignatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GetAppDetailSignatureChipsUseCase(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageInfo: PackageInfo, isApk: Boolean): List<LibStringItemChip> {
    return withContext(Dispatchers.IO) {
      runCatching {
        @Suppress("InlinedApi", "DEPRECATION")
        val flags = PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES
        if (!isApk) {
          installedAppRepository.getPackageInfo(packageInfo.packageName, flags)
            ?.getSignatures(context)
            ?: emptySequence()
        } else {
          PackageManagerCompat.getPackageArchiveInfo(packageInfo.applicationInfo!!.sourceDir, flags)!!
            .getSignatures(context)
        }
      }.onFailure {
        Timber.e(it)
      }.getOrDefault(emptySequence())
        .map {
          LibStringItemChip(it, null)
        }.toList()
    }
  }
}
