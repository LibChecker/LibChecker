package com.absinthe.libchecker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.sha256
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

object Telemetry {

  private const val UPLOAD_KEY_SHA256 = "b12e1cfbfe285849d6740018022eec2209feb28c2685df6f4d2f97dbde3b5775"

  init {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      if (isGmsCertificateException(throwable)) {
        Timber.w(throwable, "Ignored GMS GoogleCertificatesRslt SecurityException on ${thread.name}")
        return@setDefaultUncaughtExceptionHandler
      }
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  private val isAnalyticsSupported by lazy {
    val context = LibCheckerApp.app
    isGmsAvailable(context) && isOfficialPlayBuild(context)
  }

  fun setEnable(enable: Boolean) {
    if (isAnalyticsSupported) {
      runCatching {
        Firebase.analytics.setAnalyticsCollectionEnabled(enable)
      }.onFailure {
        Timber.w(it, "Failed to set analytics collection")
      }
    }
    runCatching {
      Firebase.crashlytics.setCrashlyticsCollectionEnabled(enable)
    }.onFailure {
      Timber.w(it, "Failed to set crashlytics collection")
    }
  }

  fun recordException(t: Throwable) {
    runCatching {
      Firebase.crashlytics.recordException(t)
    }.onFailure {
      Timber.w(it, "Failed to record exception")
    }
  }

  fun recordEvent(name: String, params: Map<String, Any>) {
    if (!isAnalyticsSupported) return
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
    runCatching {
      Firebase.analytics.logEvent(name.replace(" ", "_"), bundle)
    }.onFailure {
      Timber.w(it, "Failed to log analytics event")
    }
  }

  private fun isGmsAvailable(context: Context): Boolean {
    return runCatching {
      GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }.getOrDefault(false)
  }

  private fun isOfficialPlayBuild(context: Context): Boolean {
    if (BuildConfig.DEBUG) return false
    return runCatching {
      val flags = if (OsUtils.atLeastP()) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
      }
      val packageInfo = PackageManagerCompat.getPackageInfo(context.packageName, flags)
      val signatures = if (OsUtils.atLeastP()) {
        val signingInfo = packageInfo.signingInfo ?: return false
        if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
      } else {
        @Suppress("DEPRECATION")
        packageInfo.signatures
      }
      val currentSha256 = signatures?.firstOrNull()?.toByteArray()?.sha256().orEmpty()
      currentSha256.isNotEmpty() && !currentSha256.equals(UPLOAD_KEY_SHA256, ignoreCase = true)
    }.getOrDefault(false)
  }

  private fun isGmsCertificateException(throwable: Throwable?): Boolean {
    var current = throwable
    var depth = 0
    while (current != null && depth < 10) {
      if (current is SecurityException && current.message?.contains("GoogleCertificatesRslt") == true) {
        return true
      }
      current = current.cause
      depth++
    }
    return false
  }

  object Param {
    const val CONTENT_TYPE = FirebaseAnalytics.Param.CONTENT_TYPE
    const val ITEM_ID = FirebaseAnalytics.Param.ITEM_ID
    const val SUCCESS = FirebaseAnalytics.Param.SUCCESS
    const val VALUE = FirebaseAnalytics.Param.VALUE
    const val CONTENT = FirebaseAnalytics.Param.CONTENT
  }
}
