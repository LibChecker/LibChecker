package com.absinthe.libchecker.features.applist.detail

import kotlinx.coroutines.Job

class DetailLoadJobsState {
  var initSoAnalysisJob: Job? = null
  var initStaticJob: Job? = null
  var initMetaDataJob: Job? = null
  var initPermissionJob: Job? = null
  var initDexJob: Job? = null
  var initSignaturesJob: Job? = null
  var initComponentsJob: Job? = null

  fun cancelAll() {
    initSoAnalysisJob?.cancel()
    initStaticJob?.cancel()
    initMetaDataJob?.cancel()
    initPermissionJob?.cancel()
    initDexJob?.cancel()
    initSignaturesJob?.cancel()
    initComponentsJob?.cancel()
  }
}
