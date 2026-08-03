package com.absinthe.libchecker.domain.statistics.reference

import android.os.Build
import android.os.Trace
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import java.util.concurrent.atomic.AtomicInteger

internal const val TRACE_REFERENCE_BUILD_INDEX = "LC Reference buildIndex"
internal const val TRACE_REFERENCE_LOAD_BATCH = "LC Reference loadBatch"
internal const val TRACE_REFERENCE_MAP_RESULT = "LC Reference mapResult"
internal const val TRACE_REFERENCE_MATCH_RULES = "LC Reference matchRules"
internal const val TRACE_REFERENCE_RESULT_TO_FIRST_LAYOUT = "LC Reference resultToFirstLayout"
internal const val TRACE_REFERENCE_SUBMIT_RESULT = "LC Reference submitResult"

internal fun traceReferenceComputeTypeName(@LibType type: Int): String {
  val typeName = when (type) {
    NATIVE -> "NATIVE"
    SERVICE -> "SERVICE"
    ACTIVITY -> "ACTIVITY"
    RECEIVER -> "RECEIVER"
    PROVIDER -> "PROVIDER"
    PERMISSION -> "PERMISSION"
    METADATA -> "METADATA"
    PACKAGE -> "PACKAGE"
    SHARED_UID -> "SHARED_UID"
    ACTION -> "ACTION"
    else -> type.toString()
  }
  return "LC Reference compute $typeName"
}

internal inline fun <T> traceReferenceSection(sectionName: String, block: () -> T): T {
  Trace.beginSection(sectionName)
  return try {
    block()
  } finally {
    Trace.endSection()
  }
}

internal suspend inline fun <T> traceReferenceSuspendSection(
  sectionName: String,
  crossinline block: suspend () -> T
): T {
  val cookie = nextReferenceTraceCookie.incrementAndGet()
  beginReferenceAsyncSection(sectionName, cookie)
  return try {
    block()
  } finally {
    endReferenceAsyncSection(sectionName, cookie)
  }
}

internal fun beginReferenceAsyncSection(sectionName: String, cookie: Int) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    Trace.beginAsyncSection(sectionName, cookie)
  }
}

internal fun endReferenceAsyncSection(sectionName: String, cookie: Int) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    Trace.endAsyncSection(sectionName, cookie)
  }
}

@PublishedApi
internal val nextReferenceTraceCookie = AtomicInteger()
