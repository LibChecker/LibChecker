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
}
