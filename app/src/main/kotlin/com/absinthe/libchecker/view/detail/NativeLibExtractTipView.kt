package com.absinthe.libchecker.view.detail

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.span.CustomTypefaceSpan

class NativeLibExtractTipView(context: Context) : AViewGroup(context) {

  init {
    isFocusable = false
    isClickable = false
    val horizontalPadding = 16.dp
    val verticalPadding = 8.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
    setBackgroundColor(R.color.highlightComponent.getColor(context))
  }

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp)
    setImageResource(R.drawable.ic_outline_info)
    addView(this)
  }

  val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = 8.dp
    }
    val extractLibsTipPartA = context.getString(R.string.extract_native_libs_tip_part_a)
    val extractLibsTipPartB = context.getString(R.string.extract_native_libs_tip_part_b)

    val sb = SpannableString(extractLibsTipPartA + extractLibsTipPartB)
    sb.setSpan(
      CustomTypefaceSpan(Typeface.MONOSPACE),
      extractLibsTipPartA.length,
      extractLibsTipPartA.length + extractLibsTipPartB.length,
      Spannable.SPAN_INCLUSIVE_EXCLUSIVE
    )
    text = sb
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceListItemSecondary))
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    icon.autoMeasure()
    text.measure(
      (measuredWidth - paddingStart - paddingEnd - text.marginStart - text.marginEnd - icon.measuredWidth).toExactlyMeasureSpec(),
      text.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop + icon.measuredHeight.coerceAtLeast(text.measuredHeight) + paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    icon.layout(paddingStart, icon.toVerticalCenter(this))
    text.layout(icon.right + text.marginStart, text.toVerticalCenter(this))
  }
}
