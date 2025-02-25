package com.absinthe.libchecker.constant

import com.absinthe.libchecker.utils.OsUtils

object GlobalFeatures {
  // Android 7 does not support Java NIO API for reading files
  val ENABLE_DETECTING_16KB_PAGE_ALIGNMENT = OsUtils.atLeastO()
}
