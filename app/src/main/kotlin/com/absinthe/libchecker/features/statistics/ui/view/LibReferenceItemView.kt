package com.absinthe.libchecker.features.statistics.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class LibReferenceItemView(context: Context) : FrameLayout(context) {

  val container = LibReferenceItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
  }

  init {
    addView(container)
  }

  class LibReferenceItemContainerView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageButton(context).apply {
      id = android.R.id.icon
      val iconSize = context.getDimensionPixelSize(R.dimen.lib_reference_icon_size)
      layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
      setBackgroundResource(R.drawable.bg_circle_secondary_container)
      addView(this)
    }

    val labelName = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifMedium
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.marginEnd = 8.dp
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      addView(this)
    }

    val libName =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          it.marginStart = 8.dp
          it.marginEnd = 8.dp
        }
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        addView(this)
      }

    val count =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline4))
        addView(this)
      }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val labelWidth = (
        measuredWidth - paddingStart - paddingEnd -
          icon.measuredWidth - count.measuredWidth -
          labelName.marginLeft - labelName.marginRight
        )
      if (labelName.measuredWidth > labelWidth) {
        labelName.measure(labelWidth.toExactlyMeasureSpec(), labelName.defaultHeightMeasureSpec(this))
      }
      if (libName.measuredWidth > labelWidth) {
        libName.measure(labelWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
      }
      setMeasuredDimension(
        measuredWidth,
        (paddingTop + labelName.measuredHeight + libName.measuredHeight + paddingBottom)
          .coerceAtLeast(icon.measuredHeight + paddingTop + paddingBottom)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      labelName.layout(paddingStart + icon.measuredWidth + labelName.marginLeft, paddingTop)
      libName.layout(paddingStart + icon.measuredWidth + labelName.marginLeft, labelName.bottom)
      count.layout(paddingEnd, count.toVerticalCenter(this), fromRight = true)
    }
  }
}
