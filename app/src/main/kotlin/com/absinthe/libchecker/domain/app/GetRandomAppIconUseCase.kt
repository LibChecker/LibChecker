package com.absinthe.libchecker.domain.app

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class GetRandomAppIconUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(): Drawable? {
    return runCatching {
      installedAppRepository.getRandomApplicationInfo()?.loadIcon(packageManager)
    }.getOrNull()
  }
}
