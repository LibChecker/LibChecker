package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.RoundCornerView

class SnapshotItemView(context: Context) : FrameLayout(context) {

  val container = SnapshotItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
    clipToPadding = false
  }

  init {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    addView(container)
  }

  class SnapshotItemContainerView(context: Context) : RoundCornerView(context) {

    init {
      radius = 8.dp
    }

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
        ViewGroup.LayoutParams.MATCH_PARENT,
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
        layoutParams =
          LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        ViewGroup.LayoutParams.MATCH_PARENT,
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
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val targetApiInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensed
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
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
        ViewGroup.LayoutParams.MATCH_PARENT,
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
        ViewGroup.LayoutParams.MATCH_PARENT,
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
      icon.autoMeasure()
      val textWidth =
        measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - 5.dp - appName.marginStart
      appName.measure(
        textWidth.toExactlyMeasureSpec(),
        appName.defaultHeightMeasureSpec(this)
      )
      packageName.measure(
        textWidth.toExactlyMeasureSpec(),
        packageName.defaultHeightMeasureSpec(this)
      )
      versionInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        versionInfo.defaultHeightMeasureSpec(this)
      )
      packageSizeInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        packageSizeInfo.defaultHeightMeasureSpec(this)
      )
      targetApiInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        targetApiInfo.defaultHeightMeasureSpec(this)
      )
      updateTime.measure(
        textWidth.toExactlyMeasureSpec(),
        updateTime.defaultHeightMeasureSpec(this)
      )
      abiInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        abiInfo.defaultHeightMeasureSpec(this)
      )

      val packageSizeHeight = if (packageSizeInfo.isVisible) {
        packageSizeInfo.measuredHeight
      } else {
        0
      }
      val updateTimeHeight = if (updateTime.isVisible) {
        updateTime.measuredHeight
      } else {
        0
      }
      setMeasuredDimension(
        measuredWidth,
        (
          paddingTop +
            appName.measuredHeight +
            packageName.measuredHeight +
            versionInfo.measuredHeight +
            packageSizeHeight +
            targetApiInfo.measuredHeight +
            updateTimeHeight +
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
      appName.layout(icon.right + appName.marginStart, paddingTop)
      packageName.layout(appName.left, appName.bottom)
      versionInfo.layout(appName.left, packageName.bottom)
      packageSizeInfo.layout(appName.left, versionInfo.bottom)
      targetApiInfo.layout(
        appName.left,
        if (packageSizeInfo.isVisible) packageSizeInfo.bottom else versionInfo.bottom
      )
      updateTime.layout(appName.left, targetApiInfo.bottom)
      abiInfo.layout(
        appName.left,
        if (updateTime.isVisible) updateTime.bottom else targetApiInfo.bottom
      )
      stateIndicator.layout(
        paddingEnd,
        stateIndicator.toVerticalCenter(this),
        fromRight = true
      )
    }

    fun setDrawStroke(shouldDrawStroke: Boolean) {
      this.shouldDrawStroke = shouldDrawStroke
    }
  }
}
