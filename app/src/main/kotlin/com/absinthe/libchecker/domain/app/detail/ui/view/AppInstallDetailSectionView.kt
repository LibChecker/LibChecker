package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

open class AppInstallDetailSectionView(context: Context, @StringRes title: Int) : AViewGroup(context) {

  private val titleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    setText(title)
  }

  protected val container = MaterialCardView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setSmoothRoundCorner(12.dp)
    strokeWidth = 1.dp
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
  }

  init {
    addView(titleView)
    addView(container)
  }

  protected fun setContentView(content: View) {
    content.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    content.isClickable = false
    content.isLongClickable = false
    content.isFocusable = false
    content.isFocusableInTouchMode = false
    container.addView(content)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val parent = parent as ViewGroup
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    titleView.autoMeasure()
    container.measure(
      (measuredWidth - parent.paddingStart - parent.paddingEnd).toExactlyMeasureSpec(),
      if (container.isGone) 0 else container.defaultHeightMeasureSpec(parent)
    )
    setMeasuredDimension(
      measuredWidth,
      titleView.marginTop +
        titleView.measuredHeight +
        container.marginTop +
        container.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    titleView.layout(paddingStart, titleView.marginTop)
    container.layout(paddingStart, titleView.bottom + container.marginTop)
  }
}
