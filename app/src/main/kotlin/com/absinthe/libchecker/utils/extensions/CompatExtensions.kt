package com.absinthe.libchecker.utils.extensions

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.appcompat.widget.TintTypedArray
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import java.io.Closeable
import java.lang.reflect.ParameterizedType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.comparisons.reversed as kotlinReversed
import kotlin.io.use as kotlinUse

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

@Suppress("UNCHECKED_CAST")
fun <T : ViewBinding> LifecycleOwner.inflateBinding(inflater: LayoutInflater): T {
  return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments
    .filterIsInstance<Class<T>>()
    .first()
    .getDeclaredMethod("inflate", LayoutInflater::class.java)
    .invoke(null, inflater) as T
}

/**
 * [issue](https://youtrack.jetbrains.com/issue/KT-35216)
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return kotlinUse(block)
}

fun <T> Comparator<T>.reversedCompat(): Comparator<T> = kotlinReversed()

@OptIn(ExperimentalContracts::class)
@SuppressLint("RestrictedApi")
inline fun <R> TintTypedArray.use(block: (TintTypedArray) -> R): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return try {
    block(this)
  } finally {
    recycle()
  }
}
