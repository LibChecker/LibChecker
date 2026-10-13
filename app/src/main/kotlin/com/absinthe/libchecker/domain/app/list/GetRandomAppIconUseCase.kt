package com.absinthe.libchecker.domain.app.list

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository

class GetRandomAppIconUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(): Drawable? {
    return runCatching {
      installedAppRepository.getRandomApplicationInfo()
        ?.takeIf { it.icon != 0 }
        ?.loadIcon(packageManager)
    }.getOrNull()
  }
}
