package com.absinthe.libchecker.features.about

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class DeveloperItemView(context: Context) : MaterialCardView(context) {

  val container = LibReferenceItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
  }

  init {
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
    addView(container)
  }

  class LibReferenceItemContainerView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      id = android.R.id.icon
      val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
      addView(this)
    }

    val name = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.marginEnd = 8.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleMedium))
      addView(this)
    }

    val desc = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.marginEnd = 8.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
      addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val labelWidth = (
        measuredWidth - paddingStart - paddingEnd -
          icon.measuredWidth - name.marginLeft -
          name.marginRight
        )
      if (name.measuredWidth > labelWidth) {
        name.measure(
          labelWidth.toExactlyMeasureSpec(),
          name.defaultHeightMeasureSpec(this)
        )
      }
      if (desc.measuredWidth > labelWidth) {
        desc.measure(labelWidth.toExactlyMeasureSpec(), desc.defaultHeightMeasureSpec(this))
      }
      setMeasuredDimension(
        measuredWidth,
        (paddingTop + name.measuredHeight + desc.measuredHeight + paddingBottom)
          .coerceAtLeast(icon.measuredHeight + paddingTop + paddingBottom)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      name.layout(paddingStart + icon.measuredWidth + name.marginLeft, paddingTop)
      desc.layout(paddingStart + icon.measuredWidth + name.marginLeft, name.bottom)
    }
  }
}
