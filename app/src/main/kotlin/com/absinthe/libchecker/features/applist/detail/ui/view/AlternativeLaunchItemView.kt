package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup

class AlternativeLaunchItemView(context: Context) : AViewGroup(context) {

  val label =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
    }

  val className = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    addView(label)
    addView(className)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val textWidth =
      measuredWidth - paddingStart - paddingEnd
    if (label.measuredWidth > textWidth) {
      label.measure(textWidth.toExactlyMeasureSpec(), label.defaultHeightMeasureSpec(this))
    }
    if (className.measuredWidth > textWidth) {
      className.measure(textWidth.toExactlyMeasureSpec(), className.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + label.measuredHeight + className.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    label.layout(paddingStart, paddingTop)
    className.layout(paddingStart, label.bottom)
  }
}
