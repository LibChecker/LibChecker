package com.absinthe.libchecker.utils

import android.os.Bundle
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
    val bundle = Bundle()
    params.forEach { (key, value) ->
      when (value) {
        is String -> bundle.putString(key, value)
        is Int -> bundle.putInt(key, value)
        is Long -> bundle.putLong(key, value)
        is Double -> bundle.putDouble(key, value)
        is Boolean -> bundle.putBoolean(key, value)
        else -> bundle.putString(key, value.toString())
      }
    }
    Firebase.analytics.logEvent(name.replace(" ", "_"), bundle)
  }

  object Param {
    const val CONTENT_TYPE = FirebaseAnalytics.Param.CONTENT_TYPE
    const val ITEM_ID = FirebaseAnalytics.Param.ITEM_ID
    const val SUCCESS = FirebaseAnalytics.Param.SUCCESS
    const val VALUE = FirebaseAnalytics.Param.VALUE
    const val CONTENT = FirebaseAnalytics.Param.CONTENT
  }
}
