package com.absinthe.libchecker.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.absinthe.libchecker.LibCheckerApp
import timber.log.Timber

object Global {

    private val handler = Handler(Looper.getMainLooper())

    fun loop() {
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
            stack.contains("com.swift.sandhook")
        ) {
            Timber.w(e)
        } else if (stack.contains("ClipboardService")) {
            Timber.w(e)
            Toast.makeText(LibCheckerApp.context, "Cannot access to ClipboardService", Toast.LENGTH_SHORT).show()
        } else {
            throw e
        }
    }
}