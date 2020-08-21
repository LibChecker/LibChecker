package com.absinthe.libchecker.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.absinthe.libchecker.extensions.logw

object Global {

    private val handler = Handler(Looper.getMainLooper())

    fun loop() {
        handler.post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    val stack = Log.getStackTraceString(e)
                    if (stack.contains("Service.startForeground()") ||
                        stack.contains("com.swift.sandhook") ||
                        stack.contains("MainActivity.onResume")
                    ) {
                        logw(e.toString())
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}