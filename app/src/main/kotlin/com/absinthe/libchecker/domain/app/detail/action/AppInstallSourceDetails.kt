package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.utils.extensions.DexFileOptimizationInfo

data class AppInstallSourceDetails(
  val installSource: AppInstallSource?,
  val installedTime: AppInstalledTimeDisplayData?,
  val dexoptInfo: DexFileOptimizationInfo?
)

data class AppInstalledTimeDisplayData(
  val firstInstalledTime: String,
  val lastUpdatedTime: String
)
