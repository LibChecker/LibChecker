package com.absinthe.libchecker.utils.extensions

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

inline fun OnBackPressedDispatcher.addBackStateHandler(
  lifecycleOwner: LifecycleOwner,
  crossinline enabledState: () -> Boolean,
  crossinline handler: () -> Unit
) {
  val backCallback = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      isEnabled = false
      handler()
    }
  }
  this.addCallback(lifecycleOwner, backCallback)
  // update callback enabled state every 1s in the background
  val expandStateFlow = flow {
    while (true) {
      emit(enabledState())
      delay(1000)
    }
  }
  val lifecycle = lifecycleOwner.lifecycle
  expandStateFlow
    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
    .distinctUntilChanged()
    .onEach { enabled -> backCallback.isEnabled = enabled }
    .flowOn(Dispatchers.Default)
    .onEach { enabled -> Timber.d("BackStateHandler/enabled:$enabled") }
    .flowOn(Dispatchers.Main)
    .launchIn(lifecycle.coroutineScope)
}
