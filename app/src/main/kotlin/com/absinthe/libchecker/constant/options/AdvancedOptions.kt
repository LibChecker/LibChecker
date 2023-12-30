package com.absinthe.libchecker.constant.options

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
  const val SHOW_SYSTEM_FRAMEWORK_APPS = 1 shl 12
  const val SHOW_COMPILE_API = 1 shl 13

  const val MARK_EXPORTED = 1 shl 0
  const val MARK_DISABLED = 1 shl 1
  const val SHOW_MARKED_LIB = 1 shl 2

  const val DEFAULT_OPTIONS =
    SHOW_SYSTEM_APPS or
      SHOW_SYSTEM_FRAMEWORK_APPS or
      SHOW_OVERLAYS or
      SHOW_64_BIT_APPS or
      SHOW_32_BIT_APPS or
      SORT_BY_NAME or
      SHOW_TARGET_API

  const val ITEM_DEFAULT_OPTIONS =
    MARK_DISABLED or
      SHOW_MARKED_LIB
}
