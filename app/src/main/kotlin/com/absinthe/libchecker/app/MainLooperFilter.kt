package com.absinthe.libchecker.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.showToast
import com.google.android.gms.common.internal.BaseGmsClient
import timber.log.Timber

object MainLooperFilter {

  private val handler = Handler(Looper.getMainLooper())

  fun start() {
    handler.post {
      while (true) {
        try {
          Looper.loop()
        } catch (e: Throwable) {
          dealStackTraceException(e)
        }
      }
    }
  }

  private val heroes = setOf(
    "android.view.inputmethod.InputMethodManager.startInputInner",
    "com.android.server.wm.ConfigurationContainer.setActivityType",
    "com.swift.sandhook",
    "handleTopResumedActivityChanged",
    "lspatch",
    "MultiSelectPopupWindow.showMultiSelectPopupWindow",
    "Service.startForeground()",
    "tryGetViewHolderForPositionByDeadline",
    "updateForceDarkMode",
    "Expected the adapter to be 'fresh' while restoring state",
    BaseGmsClient::class.java.name
  )

  @Throws(Throwable::class)
  private fun dealStackTraceException(e: Throwable) {
    val stack = Log.getStackTraceString(e)

    if (heroes.any { stack.contains(it) }) {
      Timber.w(e)
    } else if (stack.contains("ClipboardService")) {
      Timber.w(e)
      LibCheckerApp.app.showToast("Cannot access to ClipboardService")
    } else if (stack.contains("de.robv.android.xposed")) {
      Timber.w(e)
      LibCheckerApp.app.showToast("Encounter Xposed module crash")
    } else if (stack.contains("no such table: rules_table")) {
      Repositories.deleteRulesDatabase()
      throw e
    } else {
      throw e
    }
  }
}
