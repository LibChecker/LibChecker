package com.absinthe.libchecker.domain.statistics.chart.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.AndroidVersionLabelDisplayData
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class AndroidVersionLabelView(context: Context) : AViewGroup(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  private val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = 8.dp
    }
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall))
  }

  init {
    addView(icon)
    addView(text)
  }

  fun bind(data: AndroidVersionLabelDisplayData) {
    val id = data.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android
    icon.setImageResource(id)
    text.text = data.text
    contentDescription = data.contentDescription
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
