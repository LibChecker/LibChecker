package com.absinthe.libchecker.extensions

import androidx.lifecycle.MutableLiveData

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/25
 * </pre>
 */

val <T> MutableLiveData<T>.valueUnsafe: T
    get() = this.value!!