package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class AppListRejectView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  init {
    isClickable = true
    isFocusable = true
    background = context.getDrawableByAttr(com.google.android.material.R.attr.colorSurface)
  }

  private val image = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(200.dp, 200.dp)
    setImageResource(R.drawable.ic_deny)
    addView(this)
  }

  private val text = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(300.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
      it.topMargin = 20.dp
    }
    gravity = Gravity.CENTER
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline6))
    text = context.getString(R.string.get_app_list_denied_tip)
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    image.autoMeasure()
    text.autoMeasure()
    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    image.layout(image.toHorizontalCenter(this), image.toVerticalCenter(this))
    text.layout(text.toHorizontalCenter(this), image.bottom + text.marginTop)
  }
}
