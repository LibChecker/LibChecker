package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class EmptyListView(context: Context) : AViewGroup(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(160.dp, 160.dp)
    setImageResource(R.drawable.ic_empty_list)
    addView(this)
  }

  val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    text = context.getString(R.string.empty_list)
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline5))
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    icon.autoMeasure()
    text.autoMeasure()
    setMeasuredDimension(
      measuredWidth.coerceAtLeast(
        paddingStart + icon.measuredWidth.coerceAtLeast(text.measuredWidth) + paddingEnd
      ),
      measuredHeight.coerceAtLeast(
        paddingTop + icon.measuredHeight + text.marginTop + text.measuredHeight + paddingBottom
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val top = (measuredHeight - icon.measuredHeight - text.measuredHeight - text.marginTop) / 2
    icon.layout(icon.toHorizontalCenter(this), top)
    text.layout(text.toHorizontalCenter(this), icon.bottom + text.marginTop)
  }
}
