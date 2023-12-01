package com.absinthe.libchecker.features.album.track.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.checkbox.MaterialCheckBox

class TrackItemView(context: Context) : FrameLayout(context) {

  val container = TrackItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
  }

  init {
    addView(container)
  }

  class TrackItemContainerView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
      addView(this)
    }

    val appName = AppCompatTextView(
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
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      addView(this)
    }

    val packageName =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        addView(this)
      }

    val switch = MaterialCheckBox(context).apply {
      id = android.R.id.toggle
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      background = null
    }

    init {
      addView(switch)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val textWidth =
        measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - appName.marginStart - switch.measuredWidth
      if (appName.measuredWidth > textWidth) {
        appName.measure(
          textWidth.toExactlyMeasureSpec(),
          appName.defaultHeightMeasureSpec(this)
        )
      }
      if (packageName.measuredWidth > textWidth) {
        packageName.measure(
          textWidth.toExactlyMeasureSpec(),
          packageName.defaultHeightMeasureSpec(this)
        )
      }
      setMeasuredDimension(
        measuredWidth,
        (paddingTop + appName.measuredHeight + packageName.measuredHeight + paddingBottom)
          .coerceAtLeast(icon.measuredHeight + paddingTop + paddingBottom)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      switch.layout(paddingEnd, switch.toVerticalCenter(this), fromRight = true)
      appName.layout(paddingStart + icon.measuredWidth + appName.marginStart, paddingTop)
      packageName.layout(paddingStart + icon.measuredWidth, appName.bottom)
    }
  }
}
