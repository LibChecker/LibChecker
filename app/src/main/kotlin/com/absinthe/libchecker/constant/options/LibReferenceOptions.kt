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

  const val DEFAULT_OPTIONS = SERVICES

  fun getOptionsString(options: Int): String {
    val sb = StringBuilder()
    if (options and NATIVE_LIBS != 0) {
      sb.append("NATIVE_LIBS, ")
    }
    if (options and SERVICES != 0) {
      sb.append("SERVICES, ")
    }
    if (options and ACTIVITIES != 0) {
      sb.append("ACTIVITIES, ")
    }
    if (options and RECEIVERS != 0) {
      sb.append("RECEIVERS, ")
    }
    if (options and PROVIDERS != 0) {
      sb.append("PROVIDERS, ")
    }
    if (options and PERMISSIONS != 0) {
      sb.append("PERMISSIONS, ")
    }
    if (options and METADATA != 0) {
      sb.append("METADATA, ")
    }
    if (options and PACKAGES != 0) {
      sb.append("PACKAGES, ")
    }
    if (options and SHARED_UID != 0) {
      sb.append("SHARED_UID, ")
    }
    if (options and ONLY_NOT_MARKED != 0) {
      sb.append("ONLY_NOT_MARKED, ")
    }
    if (sb.isNotEmpty()) {
      sb.delete(sb.length - 2, sb.length)
    }
    return sb.toString()
  }
}
