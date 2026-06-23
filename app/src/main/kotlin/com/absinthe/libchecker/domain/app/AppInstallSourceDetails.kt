package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo

data class AppInstallSourceDetails(
  val packageInfo: PackageInfo,
  val installSource: AppInstallSource?,
  val showInstalledTime: Boolean
)
