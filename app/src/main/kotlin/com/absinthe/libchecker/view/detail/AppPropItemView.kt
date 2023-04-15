package com.absinthe.libchecker.view.detail

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
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

  private val key = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
  }

  private val value = AppCompatTextView(
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

  init {
    addView(tip)
    addView(key)
    addView(value)
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

  fun setKeyText(text: String) {
    key.text = text
  }

  fun setValueText(text: String) {
    value.text = text
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val textWidth = measuredWidth - paddingStart - paddingEnd
    tip.measure(textWidth.toExactlyMeasureSpec(), tip.defaultHeightMeasureSpec(this))
    key.measure(textWidth.toExactlyMeasureSpec(), key.defaultHeightMeasureSpec(this))
    value.measure(textWidth.toExactlyMeasureSpec(), value.defaultHeightMeasureSpec(this))
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + tip.measuredHeight + key.measuredHeight + value.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    tip.layout(paddingStart, paddingTop)
    key.layout(paddingStart, tip.bottom)
    value.layout(paddingStart, key.bottom)
  }
}
