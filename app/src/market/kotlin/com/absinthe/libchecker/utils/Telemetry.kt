package com.absinthe.libchecker.utils

import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlin.collections.toList

object Telemetry {

  fun setEnable(enable: Boolean) {
    Firebase.analytics.setAnalyticsCollectionEnabled(enable)
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(enable)
  }

  fun recordException(t: Throwable) {
    Firebase.crashlytics.recordException(t)
  }

  fun recordEvent(name: String, params: Map<String, Any>) {
    Firebase.analytics.logEvent(name, bundleOf(*params.toList().toTypedArray()))
  }
}
