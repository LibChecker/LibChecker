package com.absinthe.libchecker.domain.app

import android.content.ComponentName
import android.content.pm.PackageManager

class SetApkAnalysisEnabledUseCase(
  private val applicationId: String,
  private val packageManager: PackageManager,
  private val apkAnalysisActivityClassName: String
) {

  operator fun invoke(enabled: Boolean): Result<Unit> {
    val componentState = if (enabled) {
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    return runCatching {
      packageManager.setComponentEnabledSetting(
        ComponentName(applicationId, apkAnalysisActivityClassName),
        componentState,
        PackageManager.DONT_KILL_APP
      )
    }
  }
}
