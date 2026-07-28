package com.absinthe.libchecker.utils.extensions

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.absinthe.libchecker.utils.Toasty
import timber.log.Timber

internal fun Context.openUrlInBrowser(url: String) {
  runCatching {
    CustomTabsIntent.Builder().build().launchUrl(this, url.toUri())
  }.onFailure {
    Timber.e(it)
  }.recoverCatching {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
  }.onFailure {
    Timber.e(it)
    Toasty.showShort(this, "No browser application")
  }
}
