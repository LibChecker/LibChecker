package com.absinthe.libchecker.constant

object AdvancedOptions {
  const val SHOW_SYSTEM_APPS = 1 shl 0
  const val SHOW_OVERLAYS = 1 shl 1
  const val SHOW_64_BIT_APPS = 1 shl 2
  const val SHOW_32_BIT_APPS = 1 shl 3
  const val SORT_BY_ASC = 1 shl 4
  const val SORT_BY_NAME = 1 shl 5
  const val SORT_BY_UPDATE_TIME = 1 shl 6
  const val SORT_BY_TARGET_API = 1 shl 7
  const val SHOW_ANDROID_VERSION = 1 shl 8
  const val SHOW_TARGET_API = 1 shl 9
  const val SHOW_MIN_API = 1 shl 10
  const val TINT_ABI_LABEL = 1 shl 11

  const val DEFAULT_OPTIONS =
    SHOW_OVERLAYS or
      SHOW_64_BIT_APPS or
      SHOW_32_BIT_APPS or
      SORT_BY_NAME or
      SHOW_TARGET_API

  fun isSortModeNothingChoose(): Boolean {
    return (GlobalValues.advancedOptions and SORT_BY_NAME) == 0 &&
      (GlobalValues.advancedOptions and SORT_BY_UPDATE_TIME) == 0 &&
      (GlobalValues.advancedOptions and SORT_BY_TARGET_API) == 0
  }
}
