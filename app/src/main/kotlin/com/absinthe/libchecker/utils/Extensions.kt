package com.absinthe.libchecker.utils

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

@MainThread
fun <T : Fragment> T.putArguments(bundle: Bundle): T {
    arguments = bundle
    return this
}

@MainThread
fun <T : Fragment> T.putArguments(vararg pairs: Pair<String, Any?>): T =
    putArguments(bundleOf(*pairs))
