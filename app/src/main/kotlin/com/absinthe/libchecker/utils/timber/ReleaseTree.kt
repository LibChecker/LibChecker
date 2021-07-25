package com.absinthe.libchecker.utils.timber

import android.util.Log
import com.absinthe.libchecker.constant.GlobalValues
import timber.log.Timber

class ReleaseTree : Timber.DebugTree() {

  override fun isLoggable(tag: String?, priority: Int): Boolean {
    return (
      !(priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) ||
        GlobalValues.debugMode
      )
  }

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    if (!isLoggable(tag, priority)) {
      return
    }
    super.log(priority, "<LC_REL>$tag", message, t)
  }
}
