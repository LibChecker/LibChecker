package com.absinthe.libchecker.view.detail

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.view.AViewGroup

class AppPropItemView(context: Context) : AViewGroup(context) {

  private val tip = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerif
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, -1)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    alpha = 0.85f
  }

  val key = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
  }

  val value = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    alpha = 0.65f
  }

  val linkToIcon = AppCompatImageButton(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp).also {
      it.marginStart = 8.dp
    }
    scaleType = ImageView.ScaleType.CENTER_CROP
    setImageResource(R.drawable.ic_outline_change_circle_24)
    setBackgroundDrawable(context.getDrawableByAttr(com.google.android.material.R.attr.selectableItemBackgroundBorderless))
    isVisible = false
  }

  init {
    addView(tip)
    addView(key)
    addView(value)
    addView(linkToIcon)
  }

  fun setTipText(text: String) {
    tip.text = text
    tip.layoutParams = tip.layoutParams.apply {
      height = if (text.isEmpty()) {
        -1
      } else {
        ViewGroup.LayoutParams.WRAP_CONTENT
      }
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val textWidth = measuredWidth - paddingStart - paddingEnd
    tip.measure(textWidth.toExactlyMeasureSpec(), tip.defaultHeightMeasureSpec(this))
    key.measure(textWidth.toExactlyMeasureSpec(), key.defaultHeightMeasureSpec(this))
    value.measure(textWidth.toExactlyMeasureSpec(), value.defaultHeightMeasureSpec(this))
    linkToIcon.autoMeasure()
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + tip.measuredHeight + key.measuredHeight + value.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    tip.layout(paddingStart, paddingTop)
    key.layout(paddingStart, tip.bottom)
    value.layout(paddingStart, key.bottom)
    linkToIcon.layout(paddingEnd, linkToIcon.toVerticalCenter(this), true)
  }
}
