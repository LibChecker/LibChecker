package com.absinthe.libchecker.features.home

import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.services.IWorkerService
import timber.log.Timber

class WorkerFeatureInitializationController {
  private var binder: IWorkerService? = null
  private var pendingRequest = false

  fun connect(binder: IWorkerService, appListStatus: Int) {
    this.binder = binder
    if (pendingRequest) {
      request(appListStatus)
    }
  }

  fun disconnect() {
    binder = null
  }

  fun request(appListStatus: Int) {
    val activeBinder = binder ?: run {
      pendingRequest = true
      return
    }
    if (appListStatus.isFeatureInitializationBlocked()) {
      pendingRequest = true
      return
    }
    pendingRequest = false
    runCatching {
      activeBinder.initFeatures()
    }.onFailure {
      Timber.w(it, "requestFeatureInitialization failed")
    }
  }

  fun getLastPackageChangedTime(): Long? {
    val activeBinder = binder ?: return null
    return runCatching {
      activeBinder.lastPackageChangedTime
    }.getOrNull()
  }

  private fun Int.isFeatureInitializationBlocked(): Boolean {
    return this == STATUS_START_INIT || this == STATUS_START_REQUEST_CHANGE
  }
}
