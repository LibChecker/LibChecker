package com.absinthe.libchecker.domain.app.update

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSelfUpdatePolicyTest {

  @Test
  fun selfUpdateIsDisabledForStableVersions() {
    assertFalse(AppSelfUpdatePolicy.isSelfUpdateEnabled(isDevVersion = false))
  }

  @Test
  fun selfUpdateRemainsEnabledForCiVersions() {
    assertTrue(AppSelfUpdatePolicy.isSelfUpdateEnabled(isDevVersion = true))
  }

  @Test
  fun supportsUserActionNotRequiredInstallOnlyOnAndroidSAndAbove() {
    assertFalse(AppSelfUpdatePolicy.supportsUserActionNotRequiredInstall(Build.VERSION_CODES.R))
    assertTrue(AppSelfUpdatePolicy.supportsUserActionNotRequiredInstall(Build.VERSION_CODES.S))
  }
}
