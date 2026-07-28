package com.absinthe.libchecker.utils.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.TintTypedArray
import java.io.File

val Context.activity: Activity?
  get() {
    var context = this
    while (true) {
      when (context) {
        is Activity -> return context
        is ContextWrapper -> context = context.baseContext
        else -> return null
      }
    }
  }

fun Context.requireAvailableCacheDir(): File {
  externalCacheDir?.takeIf { it.isDirectory || it.mkdirs() }?.let { return it }

  check(cacheDir.isDirectory || cacheDir.mkdirs()) { "Failed to create cache directory: ${cacheDir.path}" }
  return cacheDir
}

fun Context.getDimensionPixelSize(@DimenRes id: Int) = resources.getDimensionPixelSize(id)

fun Context.getColorByAttr(@AttrRes attr: Int): Int = getColorStateListByAttr(attr).defaultColor

@SuppressLint("RestrictedApi")
fun Context.getColorStateListByAttr(@AttrRes attr: Int): ColorStateList = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getColorStateList(0) }

@SuppressLint("RestrictedApi")
fun Context.getDrawableByAttr(@AttrRes attr: Int): Drawable = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getDrawable(0) }

@SuppressLint("RestrictedApi")
fun Context.getResourceIdByAttr(@AttrRes attr: Int): Int = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getResourceId(0, 0) }

@Dimension
fun Context.dpToDimension(@Dimension(unit = Dimension.DP) dp: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

@Dimension
fun Context.dpToDimension(@Dimension(unit = Dimension.DP) dp: Int) = dpToDimension(dp.toFloat())

@Dimension
fun Context.dpToDimensionPixelOffset(@Dimension(unit = Dimension.DP) dp: Float): Int = dpToDimension(dp).toInt()

@Dimension
fun Context.dpToDimensionPixelSize(@Dimension(unit = Dimension.DP) dp: Float): Int {
  val value = dpToDimension(dp)
  val size = (if (value >= 0) value + 0.5f else value - 0.5f).toInt()
  return when {
    size != 0 -> size
    value == 0f -> 0
    value > 0 -> 1
    else -> -1
  }
}

@Dimension
fun Context.dpToDimensionPixelSize(@Dimension(unit = Dimension.DP) dp: Int) = dpToDimensionPixelSize(dp.toFloat())

val Context.displayWidth: Int
  get() = resources.displayMetrics.widthPixels

@SuppressLint("RestrictedApi")
fun Context.obtainStyledAttributesCompat(
  set: AttributeSet? = null,
  @StyleableRes attrs: IntArray,
  @AttrRes defStyleAttr: Int = 0,
  @StyleRes defStyleRes: Int = 0
): TintTypedArray = TintTypedArray.obtainStyledAttributes(this, set, attrs, defStyleAttr, defStyleRes)
