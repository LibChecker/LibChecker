package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup

class AppListLoadingView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(160.dp, 160.dp)
    setImageResource(R.drawable.ic_loading_list)
    addView(this)
  }

  private val text = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    text = context.getString(R.string.loading)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    icon.autoMeasure()
    text.autoMeasure()
    setMeasuredDimension(
      icon.measuredWidth.coerceAtLeast(text.measuredWidth),
      icon.measuredHeight + text.marginTop + text.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    icon.layout(icon.toHorizontalCenter(this), 0)
    text.layout(text.toHorizontalCenter(this), icon.bottom + text.marginTop)
  }
}
