package com.absinthe.libchecker.utils.timber

import timber.log.Timber.DebugTree

class ThreadAwareDebugTree : DebugTree() {

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val newTag = tag?.let {
      "<LC_DEBUG><${Thread.currentThread().name}> $it"
    }
    super.log(priority, newTag, message, t)
  }

  override fun createStackElementTag(element: StackTraceElement): String {
    return super.createStackElementTag(element) + " (Line ${element.lineNumber})"
  }
}
