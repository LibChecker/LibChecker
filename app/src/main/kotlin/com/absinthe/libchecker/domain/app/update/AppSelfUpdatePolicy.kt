package com.absinthe.libchecker.domain.app.update

import android.os.Build

object AppSelfUpdatePolicy {

  fun isSelfUpdateEnabled(isFoss: Boolean, isDevVersion: Boolean): Boolean {
    return isFoss || isDevVersion
  }

  fun supportsUserActionNotRequiredInstall(sdkInt: Int): Boolean {
    return sdkInt >= Build.VERSION_CODES.S
  }
}
