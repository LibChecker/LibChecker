package com.absinthe.libchecker.utils.extensions

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.res.Resources
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.extensions.addPaddingEnd
import com.absinthe.libraries.utils.extensions.addPaddingStart
import com.absinthe.libraries.utils.extensions.addPaddingTop
import com.absinthe.libraries.utils.utils.UiUtils
import rikka.core.util.ClipboardUtils

fun View.setLongClickCopiedToClipboard(text: CharSequence) {
  setOnLongClickListener {
    ClipboardUtils.put(context, text)
    context.showToast(R.string.toast_copied_to_clipboard)
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
    val color = if (GlobalValues.md3Theme) {
      context.getColor(R.color.colorPrimaryMd3)
    } else {
      context.getColor(R.color.colorPrimary)
    }
    spannableString.setSpan(
      ForegroundColorSpan(color),
      start, start + highlightText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
    )
    builder.append(spannableString)
    text = builder
  }
}

fun ViewPager2.setCurrentItem(
  item: Int,
  duration: Long,
  interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
  pagePxWidth: Int = width
) {
  val pxToDrag: Int = pagePxWidth * (item - currentItem)
  val animator = ValueAnimator.ofInt(0, pxToDrag)
  var previousValue = 0
  animator.addUpdateListener { valueAnimator ->
    val currentValue = valueAnimator.animatedValue as Int
    val currentPxToDrag = (currentValue - previousValue).toFloat()
    fakeDragBy(-currentPxToDrag)
    previousValue = currentValue
  }
  animator.addListener(object : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator?) {
      beginFakeDrag()
    }

    override fun onAnimationEnd(animation: Animator?) {
      endFakeDrag()
    }

    override fun onAnimationCancel(animation: Animator?) {}
    override fun onAnimationRepeat(animation: Animator?) {}
  })
  animator.interpolator = interpolator
  animator.duration = duration
  animator.start()
}
