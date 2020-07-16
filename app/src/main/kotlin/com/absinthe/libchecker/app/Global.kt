package com.absinthe.libchecker.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.absinthe.libchecker.ktx.logw

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
                        stack.contains("requestConfiguration")
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