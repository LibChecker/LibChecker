package com.absinthe.libchecker.utils.extensions

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.extensions.addPaddingEnd
import com.absinthe.libraries.utils.extensions.addPaddingStart
import com.absinthe.libraries.utils.extensions.addPaddingTop
import com.absinthe.libraries.utils.utils.UiUtils
import java.io.File
import rikka.core.util.ClipboardUtils

fun View.setLongClickCopiedToClipboard(text: CharSequence) {
  setOnLongClickListener {
    ClipboardUtils.put(context, text)
    VersionCompat.showCopiedOnClipboardToast(context)
    true
  }
}

val Number.dp: Int get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()

var View.paddingStartCompat: Int
  set(value) {
    setPadding(value, paddingTop, paddingEnd, paddingBottom)
  }
  get() = paddingStart

fun View.addPaddingStart(padding: Int) {
  addPaddingStart(padding)
}

var View.paddingTopCompat: Int
  set(value) {
    setPadding(paddingStart, value, paddingEnd, paddingBottom)
  }
  get() = paddingTop

fun View.addPaddingTop(padding: Int) {
  addPaddingTop(padding)
}

var View.paddingEndCompat: Int
  set(value) {
    setPadding(paddingStart, paddingTop, value, paddingBottom)
  }
  get() = paddingEnd

fun View.addPaddingEnd(padding: Int) {
  addPaddingEnd(padding)
}

var View.paddingBottomCompat: Int
  set(value) {
    setPadding(paddingStart, paddingTop, paddingEnd, value)
  }
  get() = paddingBottom

fun View.addPaddingBottom(padding: Int) {
  addPaddingBottom(padding)
}

fun ViewGroup.setSystemPadding() {
  val isOrientationLandscape = context.isOrientationLandscape
  fitsSystemWindows = isOrientationLandscape
  setPadding(0, if (isOrientationLandscape) 0 else UiUtils.getStatusBarHeight(), 0, 0)
}

fun TextView.tintHighlightText(highlightText: String, rawText: CharSequence) {
  text = rawText
  if (text.contains(highlightText, true)) {
    val builder = SpannableStringBuilder()
    val spannableString = SpannableString(text.toString())
    val start = text.indexOf(highlightText, 0, true)
    val color = context.getColorByAttr(com.google.android.material.R.attr.colorPrimary)
    spannableString.setSpan(
      ForegroundColorSpan(color),
      start,
      start + highlightText.length,
      Spannable.SPAN_INCLUSIVE_EXCLUSIVE
    )
    builder.append(spannableString)
    text = builder
  }
}

fun TextView.tintTextToPrimary() {
  val builder = SpannableStringBuilder()
  val spannableString = SpannableString(text.toString())
  val color = context.getColorByAttr(com.google.android.material.R.attr.colorPrimary)
  spannableString.setSpan(
    ForegroundColorSpan(color),
    0,
    text.length,
    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
  )
  builder.append(spannableString)
  text = builder
}

fun ViewPager2.setCurrentItem(
  item: Int,
  duration: Long,
  interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
  pagePxWidth: Int = width
) {
  val pxToDrag: Int = pagePxWidth * (item - currentItem) * (if (isRtl()) -1 else 1)
  val animator = ValueAnimator.ofInt(0, pxToDrag)
  var previousValue = 0
  animator.addUpdateListener { valueAnimator ->
    val currentValue = valueAnimator.animatedValue as Int
    val currentPxToDrag = (currentValue - previousValue).toFloat()
    fakeDragBy(-currentPxToDrag)
    previousValue = currentValue
  }
  animator.addListener(object : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {
      beginFakeDrag()
    }

    override fun onAnimationEnd(animation: Animator) {
      endFakeDrag()
    }

    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
  })
  animator.interpolator = interpolator
  animator.duration = duration
  animator.start()
}

fun ViewGroup.setAlphaForAll(alpha: Float) = children.forEach {
  it.alpha = alpha
}

fun TextView.startStrikeThroughAnimation(): ValueAnimator {
  val span = SpannableString(text)
  val strikeSpan = StrikethroughSpan()
  val animator = ValueAnimator.ofInt(text.length)
  animator.addUpdateListener {
    span.setSpan(strikeSpan, 0, it.animatedValue as Int, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    text = span
    invalidate()
  }
  animator.interpolator = AccelerateDecelerateInterpolator()
  animator.duration = 1000
  animator.start()
  return animator
}

fun TextView.reverseStrikeThroughAnimation(): ValueAnimator {
  val span = SpannableString(text.toString())
  val strikeSpan = StrikethroughSpan()
  val animator = ValueAnimator.ofInt(text.length, 0)
  animator.addUpdateListener {
    span.setSpan(strikeSpan, 0, it.animatedValue as Int, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    text = span
    invalidate()
  }
  animator.interpolator = AccelerateDecelerateInterpolator()
  animator.duration = 1000
  animator.start()
  return animator
}

fun ImageView.copyToClipboard() {
  val drawable = (drawable as? BitmapDrawable)?.bitmap ?: return
  val iconFile = File(context.externalCacheDir, Constants.TEMP_ICON)
  if (!iconFile.exists()) {
    iconFile.createNewFile()
  }
  iconFile.outputStream().use {
    drawable.compress(Bitmap.CompressFormat.PNG, 100, it)
  }
  val uri = FileProvider.getUriForFile(
    context,
    BuildConfig.APPLICATION_ID + ".fileprovider",
    iconFile
  )
  if (ClipboardUtils.put(
      context,
      ClipData.newUri(context.contentResolver, Constants.TEMP_ICON, uri)
    )
  ) {
    VersionCompat.showCopiedOnClipboardToast(context)
  }
}

fun TextView.addStrikeThroughSpan() {
  val span = SpannableString(text)
  span.setSpan(StrikethroughSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  text = span
}

fun View.isRtl(): Boolean {
  return layoutDirection == View.LAYOUT_DIRECTION_RTL
}

val View.start: Int
  get() {
    return if (isRtl()) {
      right
    } else {
      left
    }
  }

val View.end: Int
  get() {
    return if (isRtl()) {
      left
    } else {
      right
    }
  }

fun View?.visibleWidth() = if (this != null && isVisible) measuredWidth else 0
fun View?.visibleHeight() = if (this != null && isVisible) measuredHeight else 0
