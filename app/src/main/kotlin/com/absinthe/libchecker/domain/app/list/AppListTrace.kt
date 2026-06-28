package com.absinthe.libchecker.domain.app.list

import android.os.Trace

internal const val TRACE_APP_LIST_APPLY_UPDATE_MAIN = "LC AppList applyUpdateMain"
internal const val TRACE_APP_LIST_BUILD_UPDATE_PLAN = "LC AppList buildUpdatePlan"
internal const val TRACE_APP_LIST_CREATE_ITEM_VIEW_STATES = "LC AppList createItemViewStates"
internal const val TRACE_APP_LIST_FILTER_ITEMS = "LC AppList filterItems"
internal const val TRACE_APP_LIST_GET_APPLICATION_MAP = "LC AppList getApplicationMap"
internal const val TRACE_APP_LIST_GET_CONTENT = "LC AppList getContent"
internal const val TRACE_APP_LIST_GET_ITEMS = "LC AppList getItems"
internal const val TRACE_APP_LIST_INITIAL_ITEM_VIEW_STATES = "LC AppList initialItemViewStates"
internal const val TRACE_APP_LIST_ITEM_VIEW_STATES = "LC AppList itemViewStates"
internal const val TRACE_APP_LIST_PACKAGE_STATES = "LC AppList packageStates"
internal const val TRACE_APP_LIST_REMAINING_ITEM_VIEW_STATES = "LC AppList remainingItemViewStates"
internal const val TRACE_APP_LIST_RESOLVE_PACKAGE_STATES = "LC AppList resolvePackageStates"

internal inline fun <T> traceAppListSection(sectionName: String, block: () -> T): T {
  Trace.beginSection(sectionName)
  return try {
    block()
  } finally {
    Trace.endSection()
  }
}

internal suspend inline fun <T> traceAppListSuspendSection(
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
