package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.domain.app.InstalledAppRepository

class GetApexPackageNamesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(): Set<String> {
    return installedAppRepository.getApexPackageNames()
  }
}
