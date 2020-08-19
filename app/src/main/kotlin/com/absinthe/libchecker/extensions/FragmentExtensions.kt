package com.absinthe.libchecker.extensions

import androidx.annotation.*
import androidx.fragment.app.Fragment

fun Fragment.getAnimation(@AnimRes id: Int) = requireContext().getAnimation(id)

fun Fragment.getBoolean(@BoolRes id: Int) = requireContext().getBoolean(id)

fun Fragment.getDimension(@DimenRes id: Int) = requireContext().getDimension(id)

fun Fragment.getDimensionPixelOffset(@DimenRes id: Int) =
    requireContext().getDimensionPixelOffset(id)

fun Fragment.getDimensionPixelSize(@DimenRes id: Int) = requireContext().getDimensionPixelSize(id)

fun Fragment.getFloat(@DimenRes id: Int) = requireContext().getFloat(id)

fun Fragment.getInteger(@IntegerRes id: Int) = requireContext().getInteger(id)

fun Fragment.getInterpolator(@InterpolatorRes id: Int) = requireContext().getInterpolator(id)

fun Fragment.getQuantityString(@PluralsRes id: Int, quantity: Int): String =
    requireContext().getQuantityString(id, quantity)

fun Fragment.getQuantityString(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any?
): String = requireContext().getQuantityString(id, quantity, *formatArgs)

fun Fragment.getQuantityText(@PluralsRes id: Int, quantity: Int): CharSequence =
    requireContext().getQuantityText(id, quantity)

fun Fragment.getStringArray(@ArrayRes id: Int) = requireContext().getStringArray(id)

fun Fragment.getBooleanByAttr(@AttrRes attr: Int) = requireContext().getBooleanByAttr(attr)

fun Fragment.getColorByAttr(@AttrRes attr: Int) = requireContext().getColorByAttr(attr)

fun Fragment.getColorStateListByAttr(@AttrRes attr: Int) =
    requireContext().getColorStateListByAttr(attr)

fun Fragment.getDimensionByAttr(@AttrRes attr: Int) = requireContext().getDimensionByAttr(attr)

fun Fragment.getDimensionPixelOffsetByAttr(@AttrRes attr: Int) =
    requireContext().getDimensionPixelOffsetByAttr(attr)

fun Fragment.getDimensionPixelSizeByAttr(@AttrRes attr: Int): Int =
    requireContext().getDimensionPixelSizeByAttr(attr)

fun Fragment.getDrawableByAttr(@AttrRes attr: Int) = requireContext().getDrawableByAttr(attr)

fun Fragment.getFloatByAttr(@AttrRes attr: Int) = requireContext().getFloatByAttr(attr)

fun Fragment.getResourceIdByAttr(@AttrRes attr: Int): Int =
    requireContext().getResourceIdByAttr(attr)

@Dimension
fun Fragment.dpToDimension(@Dimension(unit = Dimension.DP) dp: Float) =
    requireContext().dpToDimension(dp)

@Dimension
fun Fragment.dpToDimension(@Dimension(unit = Dimension.DP) dp: Int) =
    requireContext().dpToDimension(dp)

@Dimension
fun Fragment.dpToDimensionPixelOffset(@Dimension(unit = Dimension.DP) dp: Float) =
    requireContext().dpToDimensionPixelOffset(dp)

@Dimension
fun Fragment.dpToDimensionPixelOffset(@Dimension(unit = Dimension.DP) dp: Int) =
    requireContext().dpToDimensionPixelOffset(dp)

@Dimension
fun Fragment.dpToDimensionPixelSize(@Dimension(unit = Dimension.DP) dp: Float) =
    requireContext().dpToDimensionPixelSize(dp)

@Dimension
fun Fragment.dpToDimensionPixelSize(@Dimension(unit = Dimension.DP) dp: Int) =
    requireContext().dpToDimensionPixelSize(dp)

val Fragment.shortAnimTime
    get() = requireContext().shortAnimTime

val Fragment.mediumAnimTime
    get() = requireContext().mediumAnimTime

val Fragment.longAnimTime
    get() = requireContext().longAnimTime