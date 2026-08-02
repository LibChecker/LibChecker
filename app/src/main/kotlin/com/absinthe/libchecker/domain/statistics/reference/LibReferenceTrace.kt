package com.absinthe.libchecker.domain.statistics.reference

import android.os.Build
import android.os.Trace
import java.util.concurrent.atomic.AtomicInteger

internal const val TRACE_REFERENCE_BUILD_INDEX = "LC Reference buildIndex"
internal const val TRACE_REFERENCE_LOAD_BATCH = "LC Reference loadBatch"
internal const val TRACE_REFERENCE_MAP_RESULT = "LC Reference mapResult"
internal const val TRACE_REFERENCE_MATCH_RULES = "LC Reference matchRules"
internal const val TRACE_REFERENCE_RESULT_TO_FIRST_LAYOUT = "LC Reference resultToFirstLayout"
internal const val TRACE_REFERENCE_SUBMIT_RESULT = "LC Reference submitResult"

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
