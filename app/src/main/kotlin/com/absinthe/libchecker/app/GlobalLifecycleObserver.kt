package com.absinthe.libchecker.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.constant.GlobalValues

class GlobalLifecycleObserver : DefaultLifecycleObserver {

  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)
    GlobalValues.shouldRequestChange.value = true
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
    GlobalValues.shouldRequestChange.value = true
  }
}
