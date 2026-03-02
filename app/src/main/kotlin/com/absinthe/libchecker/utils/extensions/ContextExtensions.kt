package com.absinthe.libchecker.utils.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.annotation.AnimRes
import androidx.annotation.ArrayRes
import androidx.annotation.AttrRes
import androidx.annotation.BoolRes
import androidx.annotation.DimenRes
import androidx.annotation.Dimension
import androidx.annotation.IntegerRes
import androidx.annotation.InterpolatorRes
import androidx.annotation.PluralsRes
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.TintTypedArray
import androidx.core.content.res.ResourcesCompat

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

fun Context.getAnimation(@AnimRes id: Int): Animation = AnimationUtils.loadAnimation(this, id)

fun Context.getBoolean(@BoolRes id: Int) = resources.getBoolean(id)

fun Context.getDimension(@DimenRes id: Int) = resources.getDimension(id)

fun Context.getDimensionPixelOffset(@DimenRes id: Int) = resources.getDimensionPixelOffset(id)

fun Context.getDimensionPixelSize(@DimenRes id: Int) = resources.getDimensionPixelSize(id)

fun Context.getFloat(@DimenRes id: Int) = resources.getFloatCompat(id)

fun Context.getInteger(@IntegerRes id: Int) = resources.getInteger(id)

fun Context.getInterpolator(@InterpolatorRes id: Int): Interpolator = AnimationUtils.loadInterpolator(this, id)

fun Context.getQuantityString(@PluralsRes id: Int, quantity: Int): String = resources.getQuantityString(id, quantity)

fun Context.getQuantityString(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any?): String = resources.getQuantityString(id, quantity, *formatArgs)

fun Context.getQuantityText(@PluralsRes id: Int, quantity: Int): CharSequence = resources.getQuantityText(id, quantity)

fun Context.getStringArray(@ArrayRes id: Int): Array<String> = resources.getStringArray(id)

@SuppressLint("RestrictedApi")
fun Context.getBooleanByAttr(@AttrRes attr: Int): Boolean = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getBoolean(0, false) }

fun Context.getColorByAttr(@AttrRes attr: Int): Int = getColorStateListByAttr(attr).defaultColor

@SuppressLint("RestrictedApi")
fun Context.getColorStateListByAttr(@AttrRes attr: Int): ColorStateList = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getColorStateList(0) }

@SuppressLint("RestrictedApi")
fun Context.getDimensionByAttr(@AttrRes attr: Int): Float = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getDimension(0, 0f) }

@SuppressLint("RestrictedApi")
fun Context.getDimensionPixelOffsetByAttr(@AttrRes attr: Int): Int = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use {
  it.getDimensionPixelOffset(0, 0)
}

@SuppressLint("RestrictedApi")
fun Context.getDimensionPixelSizeByAttr(@AttrRes attr: Int): Int = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getDimensionPixelSize(0, 0) }

@SuppressLint("RestrictedApi")
fun Context.getDrawableByAttr(@AttrRes attr: Int): Drawable = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getDrawable(0) }

@SuppressLint("RestrictedApi")
fun Context.getFloatByAttr(@AttrRes attr: Int): Float = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getFloat(0, 0f) }

@SuppressLint("RestrictedApi")
fun Context.getResourceIdByAttr(@AttrRes attr: Int): Int = obtainStyledAttributesCompat(attrs = intArrayOf(attr)).use { it.getResourceId(0, 0) }

@Dimension
fun Context.dpToDimension(@Dimension(unit = Dimension.DP) dp: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

@Dimension
fun Context.dpToDimension(@Dimension(unit = Dimension.DP) dp: Int) = dpToDimension(dp.toFloat())

@Dimension
fun Context.dpToDimensionPixelOffset(@Dimension(unit = Dimension.DP) dp: Float): Int = dpToDimension(dp).toInt()

@Dimension
fun Context.dpToDimensionPixelOffset(@Dimension(unit = Dimension.DP) dp: Int) = dpToDimensionPixelOffset(dp.toFloat())

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

val Context.shortAnimTime: Int
  get() = getInteger(android.R.integer.config_shortAnimTime)

val Context.mediumAnimTime: Int
  get() = getInteger(android.R.integer.config_mediumAnimTime)

val Context.longAnimTime: Int
  get() = getInteger(android.R.integer.config_longAnimTime)

val Context.displayWidth: Int
  get() = resources.displayMetrics.widthPixels

val Context.displayHeight: Int
  get() = resources.displayMetrics.heightPixels

fun Context.hasSwDp(@Dimension(unit = Dimension.DP) dp: Int): Boolean = resources.configuration.smallestScreenWidthDp >= dp

val Context.hasSw600Dp: Boolean
  get() = hasSwDp(600)

fun Context.hasWDp(@Dimension(unit = Dimension.DP) dp: Int): Boolean = resources.configuration.screenWidthDp >= dp

val Context.hasW600Dp: Boolean
  get() = hasWDp(600)

val Context.hasW960Dp: Boolean
  get() = hasWDp(960)

val Context.isOrientationLandscape: Boolean
  get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

val Context.isOrientationPortrait: Boolean
  get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

val Context.isLightTheme: Boolean
  get() = getBooleanByAttr(androidx.appcompat.R.attr.isLightTheme)

val Context.layoutInflater: LayoutInflater
  get() = LayoutInflater.from(this)

fun Context.withTheme(@StyleRes themeRes: Int): Context = if (themeRes != 0) ContextThemeWrapper(this, themeRes) else this

fun Resources.getFloatCompat(@DimenRes id: Int) = ResourcesCompat.getFloat(this, id)

@SuppressLint("RestrictedApi")
fun Context.obtainStyledAttributesCompat(
  set: AttributeSet? = null,
  @StyleableRes attrs: IntArray,
  @AttrRes defStyleAttr: Int = 0,
  @StyleRes defStyleRes: Int = 0
): TintTypedArray = TintTypedArray.obtainStyledAttributes(this, set, attrs, defStyleAttr, defStyleRes)
