package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.domain.app.InstalledAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetApexPackageNamesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(): Set<String> = withContext(Dispatchers.IO) {
    installedAppRepository.getApexPackageNames()
  }
}
