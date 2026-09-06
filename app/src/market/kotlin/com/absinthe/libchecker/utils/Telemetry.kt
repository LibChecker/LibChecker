package com.absinthe.libchecker.utils

import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics

object Telemetry {

  fun setEnable(enable: Boolean) {
    Firebase.analytics.setAnalyticsCollectionEnabled(enable)
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(enable)
  }

  fun recordException(t: Throwable) {
    Firebase.crashlytics.recordException(t)
  }

  fun recordEvent(name: String, params: Map<String, Any>) {
    Firebase.analytics.logEvent(name.replace(" ", "_"), bundleOf(*params.toList().toTypedArray()))
  }

  object Param {
    const val CONTENT_TYPE = FirebaseAnalytics.Param.CONTENT_TYPE
    const val ITEM_ID = FirebaseAnalytics.Param.ITEM_ID
    const val SUCCESS = FirebaseAnalytics.Param.SUCCESS
    const val VALUE = FirebaseAnalytics.Param.VALUE
    const val CONTENT = FirebaseAnalytics.Param.CONTENT
  }
}
