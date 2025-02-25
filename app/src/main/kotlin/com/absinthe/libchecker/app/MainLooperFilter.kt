package com.absinthe.libchecker.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.utils.showToast
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
    "addChildrenForExpandedActionView",
    "android.view.inputmethod.InputMethodManager.startInputInner",
    "com.android.server.wm.ConfigurationContainer.setActivityType",
    "com.google.android.gms.common.internal.BaseGmsClient.getRemoteService",
    "com.swift.sandhook",
    "handleTopResumedActivityChanged",
    "lspatch",
    "MultiSelectPopupWindow.showMultiSelectPopupWindow",
    "Service.startForeground()",
    "tryGetViewHolderForPositionByDeadline",
    "updateForceDarkMode"
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
    } else {
      throw e
    }
  }
}
