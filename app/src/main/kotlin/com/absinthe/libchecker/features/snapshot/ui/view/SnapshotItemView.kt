package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.visibleHeight
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class SnapshotItemView(context: Context) : MaterialCardView(context) {

  val container = SnapshotItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
  }

  init {
    layoutParams = ViewGroup.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    addView(container)
  }

  class SnapshotItemContainerView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = LayoutParams(iconSize, iconSize)
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
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
      addView(this)
    }

    val packageName =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        addView(this)
      }

    val versionInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensed
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val packageSizeInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensed
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val apisInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensed
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val abiInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensedMedium
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      clipChildren = false
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val updateTime = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensed
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val stateIndicator = SnapshotStateIndicatorView(context).apply {
      layoutParams = LayoutParams(5.dp, ViewGroup.LayoutParams.MATCH_PARENT)
      addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val textWidth =
        measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - 5.dp - appName.marginStart
      if (appName.measuredWidth > textWidth) {
        appName.measure(textWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this))
      }
      if (packageName.measuredWidth > textWidth) {
        packageName.measure(textWidth.toExactlyMeasureSpec(), packageName.defaultHeightMeasureSpec(this))
      }
      if (versionInfo.measuredWidth > textWidth) {
        versionInfo.measure(textWidth.toExactlyMeasureSpec(), versionInfo.defaultHeightMeasureSpec(this))
      }
      if (packageSizeInfo.measuredWidth > textWidth) {
        packageSizeInfo.measure(textWidth.toExactlyMeasureSpec(), packageSizeInfo.defaultHeightMeasureSpec(this))
      }
      if (apisInfo.measuredWidth > textWidth) {
        apisInfo.measure(textWidth.toExactlyMeasureSpec(), apisInfo.defaultHeightMeasureSpec(this))
      }
      if (updateTime.measuredWidth > textWidth) {
        updateTime.measure(textWidth.toExactlyMeasureSpec(), updateTime.defaultHeightMeasureSpec(this))
      }
      if (abiInfo.measuredWidth > textWidth) {
        abiInfo.measure(textWidth.toExactlyMeasureSpec(), abiInfo.defaultHeightMeasureSpec(this))
      }

      setMeasuredDimension(
        measuredWidth,
        (
          paddingTop +
            appName.measuredHeight +
            packageName.measuredHeight +
            versionInfo.measuredHeight +
            packageSizeInfo.visibleHeight() +
            apisInfo.measuredHeight +
            updateTime.visibleHeight() +
            abiInfo.measuredHeight +
            paddingBottom
          )
      )
      stateIndicator.measure(
        stateIndicator.defaultWidthMeasureSpec(this),
        (measuredHeight - paddingTop - paddingBottom).toExactlyMeasureSpec()
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      val appNameXOffset = paddingStart + icon.measuredWidth + appName.marginStart
      appName.layout(appNameXOffset, paddingTop)
      packageName.layout(appNameXOffset, appName.bottom)
      versionInfo.layout(appNameXOffset, packageName.bottom)
      packageSizeInfo.layout(appNameXOffset, versionInfo.bottom)
      apisInfo.layout(
        appNameXOffset,
        if (packageSizeInfo.isVisible) packageSizeInfo.bottom else versionInfo.bottom
      )
      updateTime.layout(appNameXOffset, apisInfo.bottom)
      abiInfo.layout(
        appNameXOffset,
        if (updateTime.isVisible) updateTime.bottom else apisInfo.bottom
      )
      stateIndicator.layout(
        paddingEnd,
        stateIndicator.toVerticalCenter(this),
        fromRight = true
      )
    }
  }
}
