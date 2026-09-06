package com.absinthe.libchecker.utils

object Telemetry {

  fun setEnable(enable: Boolean) {
    // Firebase.analytics.setAnalyticsCollectionEnabled(false)
    // Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
  }

  fun recordException(t: Throwable) {
    // Firebase.crashlytics.recordException(t)
  }

  fun recordEvent(name: String, params: Map<String, Any>) {
    // Firebase.analytics.logEvent(name, params)
  }

  object Param {
    const val CONTENT_TYPE = ""
    const val ITEM_ID = ""
    const val SUCCESS = ""
    const val VALUE = ""
    const val CONTENT = ""
  }
}
