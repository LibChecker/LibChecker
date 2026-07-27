package com.absinthe.libchecker.domain.app.update

import android.os.Build

object AppSelfUpdatePolicy {

  fun isSelfUpdateEnabled(isFoss: Boolean, isDevVersion: Boolean): Boolean {
    return isFoss || isDevVersion
  }

  internal fun defaultUpdateChannel(isDevVersion: Boolean): AppUpdateChannel {
    return if (isDevVersion) AppUpdateChannel.CI else AppUpdateChannel.STABLE
  }

  fun supportsUserActionNotRequiredInstall(sdkInt: Int): Boolean {
    return sdkInt >= Build.VERSION_CODES.S
  }
}
