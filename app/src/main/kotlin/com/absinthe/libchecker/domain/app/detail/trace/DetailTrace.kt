package com.absinthe.libchecker.domain.app.detail.trace

import android.os.Trace

internal const val TRACE_DETAIL_GET_PACKAGE_INFO = "LC Detail getPackageInfo"
internal const val TRACE_DETAIL_LOAD_PACKAGE = "LC Detail loadPackage"
internal const val TRACE_DETAIL_NATIVE_ACTIVITY_NAMES = "LC Detail nativeActivityNames"
internal const val TRACE_DETAIL_NATIVE_CHIP_LIST = "LC Detail nativeChipList"
internal const val TRACE_DETAIL_NATIVE_INIT_USE_CASE = "LC Detail nativeInitUseCase"
internal const val TRACE_DETAIL_NATIVE_LIBS = "LC Detail nativeLibs"
internal const val TRACE_DETAIL_NATIVE_RULE_MATCH = "LC Detail nativeRuleMatch"
internal const val TRACE_DETAIL_NATIVE_SET_ITEMS = "LC Detail nativeSetItems"
internal const val TRACE_DETAIL_PARSE_SELECTED_ABI = "LC Detail parseSelectedAbi"
internal const val TRACE_DETAIL_PRELOAD_NATIVE_LIB_NAMES = "LC Detail preloadNativeLibNames"
internal const val TRACE_DETAIL_SOURCE_LIBS = "LC Detail sourceLibs"
internal const val TRACE_DETAIL_SUPPORTS_16KB = "LC Detail supports16KB"

internal inline fun <T> traceDetailSection(sectionName: String, block: () -> T): T {
  Trace.beginSection(sectionName)
  return try {
    block()
  } finally {
    Trace.endSection()
  }
}

internal suspend inline fun <T> traceDetailSuspendSection(
  sectionName: String,
  crossinline block: suspend () -> T
): T {
  Trace.beginSection(sectionName)
  return try {
    block()
  } finally {
    Trace.endSection()
  }
}
