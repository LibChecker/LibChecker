package com.absinthe.libchecker.utils

class TimeRecorder {

    private var startTime: Long = 0L
    private var endTime: Long = 0L

    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun end() {
        endTime = System.currentTimeMillis()
    }

    override fun toString(): String {
        return "Time consumed: ${endTime - startTime}ms."
    }
}