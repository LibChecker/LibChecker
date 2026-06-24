package com.absinthe.libchecker.features.applist.detail

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DetailLoadJobsState {
  private val jobs = mutableMapOf<Key, Job>()

  fun cancelAll() {
    jobs.values.forEach { it.cancel() }
    jobs.clear()
  }

  fun cancel(key: Key) {
    jobs[key]?.cancel()
    jobs.remove(key)
  }

  fun launchIfNeeded(
    key: Key,
    scope: CoroutineScope,
    hasData: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend CoroutineScope.() -> Unit
  ) {
    val currentJob = jobs[key]
    if (currentJob?.isActive == true || hasData) {
      return
    }
    jobs[key] = scope.launch(dispatcher, block = block)
  }

  enum class Key {
    NATIVE_LIBS,
    STATIC_LIBS,
    METADATA,
    PERMISSIONS,
    DEX,
    SIGNATURES,
    COMPONENTS
  }
}
