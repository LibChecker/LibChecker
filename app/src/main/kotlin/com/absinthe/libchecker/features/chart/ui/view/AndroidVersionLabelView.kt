package com.absinthe.libchecker.features.chart.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class AndroidVersionLabelView(context: Context) : AViewGroup(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp)
    addView(this)
  }

  val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = 8.dp
    }
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
    addView(this)
  }

  fun setIcon(resId: Int?) {
    val id = resId ?: com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android
    icon.setImageResource(id)
    if (icon.parent == null) {
      addView(icon)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    icon.autoMeasure()
    text.autoMeasure()
    setMeasuredDimension(
      measuredWidth,
      paddingTop + icon.measuredHeight.coerceAtLeast(text.measuredHeight) + paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val iconOffsetX = (measuredWidth - (icon.measuredWidth + text.marginStart + text.measuredWidth)) / 2
    icon.layout(iconOffsetX, icon.toVerticalCenter(this))
    text.layout(iconOffsetX + icon.measuredWidth + text.marginStart, text.toVerticalCenter(this))
  }
}
