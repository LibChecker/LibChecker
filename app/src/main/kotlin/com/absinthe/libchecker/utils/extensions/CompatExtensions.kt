package com.absinthe.libchecker.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.TintTypedArray
import androidx.core.content.ContextCompat
import java.io.Closeable
import kotlin.comparisons.reversed as kotlinReversed
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.use as kotlinUse

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

@ColorInt
fun @receiver:ColorRes Int.getColor(context: Context): Int = ContextCompat.getColor(context, this)

fun @receiver:DrawableRes Int.getDrawable(context: Context): Drawable? = ContextCompat.getDrawable(context, this)

fun @receiver:ColorRes Int.toColorStateList(context: Context): ColorStateList {
  return ColorStateList.valueOf(getColor(context))
}

fun @receiver:ColorInt Int.toColorStateListByColor(): ColorStateList {
  return ColorStateList.valueOf(this)
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
