package com.absinthe.libchecker.data.statistics

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.statistics.reference.repository.PermissionLabelResolver

class AndroidPermissionLabelResolver(
  private val packageManager: PackageManager
) : PermissionLabelResolver {

  override fun resolve(permissionName: String): String? {
    val normalizedName = permissionName.substringBefore(" ")
    return runCatching {
      packageManager
        .getPermissionInfo(normalizedName, 0)
        .loadLabel(packageManager)
        .toString()
        .takeIf { it.isNotBlank() && it != normalizedName }
    }.getOrNull()
  }
}
