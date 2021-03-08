package com.absinthe.libchecker.utils.timber

import timber.log.Timber.DebugTree

class ThreadAwareDebugTree : DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        var newTag = tag

        newTag?.let {
            val threadName = Thread.currentThread().name
            newTag = "<$threadName> $tag"
        }

        super.log(priority, newTag, message, t)
    }

    override fun createStackElementTag(element: StackTraceElement): String {
        return super.createStackElementTag(element) + " (Line ${element.lineNumber})"
    }
}