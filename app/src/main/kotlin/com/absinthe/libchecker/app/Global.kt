package com.absinthe.libchecker.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.extensions.logw

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
            logw(e.toString())
        } else if (stack.contains("ClipboardService")) {
            logw(e.toString())
            Toast.makeText(LibCheckerApp.context, "Cannot access to ClipboardService", Toast.LENGTH_SHORT).show()
        } else {
            throw e
        }
    }
}