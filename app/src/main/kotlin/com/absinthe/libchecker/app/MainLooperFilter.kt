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

  @Throws(Throwable::class)
  private fun dealStackTraceException(e: Throwable) {
    val stack = Log.getStackTraceString(e)

    if (stack.contains("Service.startForeground()") ||
      stack.contains("com.swift.sandhook") ||
      stack.contains("updateForceDarkMode") ||
      stack.contains("MultiSelectPopupWindow.showMultiSelectPopupWindow") ||
      stack.contains("android.view.inputmethod.InputMethodManager.startInputInner") ||
      stack.contains("com.android.server.wm.ConfigurationContainer.setActivityType") ||
      stack.contains("handleTopResumedActivityChanged") ||
      stack.contains("lspatch") ||
      stack.contains("tryGetViewHolderForPositionByDeadline")
    ) {
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
