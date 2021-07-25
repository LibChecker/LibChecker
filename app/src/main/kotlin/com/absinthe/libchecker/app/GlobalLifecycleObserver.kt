package com.absinthe.libchecker.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.absinthe.libchecker.constant.GlobalValues

class GlobalLifecycleObserver : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        GlobalValues.shouldRequestChange.value = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        GlobalValues.shouldRequestChange.value = true
    }
}
