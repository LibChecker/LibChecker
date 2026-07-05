package com.absinthe.libchecker.domain.app.detail.action

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

data class ProviderPermissionItem(
  val packageName: String,
  val providerName: String
)

class GetPermissionProvidersUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(permissionName: String): List<ProviderPermissionItem> {
    val apps = installedAppRepository.getApplicationList()
    val result = mutableListOf<ProviderPermissionItem>()
    val seenPackages = mutableSetOf<String>()
    val coroutineContext = currentCoroutineContext()

    for (app in apps) {
      if (!coroutineContext.isActive) {
        return result
      }

      val packageInfo = runCatching {
        installedAppRepository.getPackageInfo(
          app.packageName,
          PackageManager.GET_PROVIDERS
        )
      }.onFailure {
        Timber.w(it, "Failed to get package info for ${app.packageName}")
      }.getOrNull() ?: continue

      val providers = packageInfo.providers ?: continue

      for (provider in providers) {
        if (provider.readPermission == permissionName ||
          provider.writePermission == permissionName
        ) {
          if (seenPackages.add(app.packageName)) {
            result.add(
              ProviderPermissionItem(
                packageName = app.packageName,
                providerName = provider.name
              )
            )
          }
          break
        }
      }
    }

    return result
  }
}
