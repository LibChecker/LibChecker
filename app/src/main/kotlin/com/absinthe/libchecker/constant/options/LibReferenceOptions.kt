package com.absinthe.libchecker.constant.options

object LibReferenceOptions {
  const val NATIVE_LIBS = 1 shl 0
  const val SERVICES = 1 shl 1
  const val ACTIVITIES = 1 shl 2
  const val RECEIVERS = 1 shl 3
  const val PROVIDERS = 1 shl 4
  const val PERMISSIONS = 1 shl 5
  const val METADATA = 1 shl 6
  const val PACKAGES = 1 shl 7
  const val SHARED_UID = 1 shl 8
  const val ONLY_NOT_MARKED = 1 shl 9
  const val ACTION = 1 shl 10

  const val DEFAULT_OPTIONS =
    NATIVE_LIBS or
      SERVICES or
      ACTIVITIES or
      RECEIVERS or
      PROVIDERS or
      ACTION

  fun getOptionsString(options: Int) = buildString {
    if (options and NATIVE_LIBS != 0) {
      append("NATIVE_LIBS, ")
    }
    if (options and SERVICES != 0) {
      append("SERVICES, ")
    }
    if (options and ACTIVITIES != 0) {
      append("ACTIVITIES, ")
    }
    if (options and RECEIVERS != 0) {
      append("RECEIVERS, ")
    }
    if (options and PROVIDERS != 0) {
      append("PROVIDERS, ")
    }
    if (options and PERMISSIONS != 0) {
      append("PERMISSIONS, ")
    }
    if (options and METADATA != 0) {
      append("METADATA, ")
    }
    if (options and PACKAGES != 0) {
      append("PACKAGES, ")
    }
    if (options and SHARED_UID != 0) {
      append("SHARED_UID, ")
    }
    if (options and ONLY_NOT_MARKED != 0) {
      append("ONLY_NOT_MARKED, ")
    }
    if (options and ACTION != 0) {
      append("ACTION, ")
    }
    if (isNotEmpty()) {
      delete(length - 2, length)
    }
  }
}
