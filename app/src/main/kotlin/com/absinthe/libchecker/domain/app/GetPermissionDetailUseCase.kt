package com.absinthe.libchecker.domain.app

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.utils.extensions.getAppName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GetPermissionDetailUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(permissionName: String): AppPermissionDetail = withContext(Dispatchers.IO) {
    val normalizedName = permissionName.substringBefore(" ")
    val permissionInfo = runCatching {
      packageManager.getPermissionInfo(normalizedName, 0)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()

    AppPermissionDetail(
      name = normalizedName,
      icon = permissionInfo?.loadIconOrNull(),
      label = permissionInfo?.loadLabelOrNull(),
      description = permissionInfo?.loadDescriptionOrNull(),
      providerAppName = permissionInfo?.packageName?.let(::getProviderAppName)
    )
  }

  private fun PermissionInfo.loadIconOrNull(): Drawable? {
    if (icon == 0) {
      return null
    }
    return runCatching {
      loadIcon(packageManager)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private fun PermissionInfo.loadLabelOrNull(): CharSequence? {
    return runCatching {
      loadLabel(packageManager).takeIf(CharSequence::isNotEmpty)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private fun PermissionInfo.loadDescriptionOrNull(): CharSequence? {
    return runCatching {
      loadDescription(packageManager)?.takeIf(CharSequence::isNotEmpty)
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private fun getProviderAppName(packageName: String): String? {
    return installedAppRepository.getPackageInfo(packageName)?.getAppName(packageManager)
  }
}

data class AppPermissionDetail(
  val name: String,
  val icon: Drawable?,
  val label: CharSequence?,
  val description: CharSequence?,
  val providerAppName: String?
)
