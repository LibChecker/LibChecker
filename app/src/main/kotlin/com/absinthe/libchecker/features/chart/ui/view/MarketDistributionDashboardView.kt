package com.absinthe.libchecker.features.chart.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginEnd
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.chip.Chip

class MarketDistributionDashboardView(context: Context) : AViewGroup(context) {

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    val verticalPadding = 4.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
  }

  val title =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginEnd = context.getDimensionPixelSize(R.dimen.normal_padding)
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      setText(R.string.android_dist_title)
      addView(this)
    }

  val subtitle =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      addView(this)
    }

  val chip = Chip(context).apply {
    isClickable = false
    layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
    chipIcon = context.getDrawable(R.drawable.ic_open_in_new)
    setText(R.string.android_dist_source)
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val chipWidth = chip.measuredWidth + title.marginEnd
    val libNameWidth = measuredWidth - paddingStart - paddingEnd - title.marginEnd - chipWidth
    if (title.measuredWidth > libNameWidth) {
      title.measure(libNameWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
    }
    if (subtitle.measuredWidth > libNameWidth) {
      subtitle.measure(libNameWidth.toExactlyMeasureSpec(), subtitle.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      (title.measuredHeight + subtitle.measuredHeight + paddingTop + paddingBottom).coerceAtLeast(
        40.dp
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    title.layout(paddingStart, paddingTop)
    subtitle.layout(paddingStart, title.bottom)
    chip.layout(paddingEnd, chip.toVerticalCenter(this), fromRight = true)
  }
}
