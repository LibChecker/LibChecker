package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class SnapshotItemView(context: Context) : MaterialCardView(context) {

  val container = SnapshotItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    clipToPadding = false
  }

  init {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
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
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
      addView(this)
    }

    val packageName =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
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
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      addView(this)
    }

    val stateIndicator = SnapshotStateIndicatorView(context).apply {
      layoutParams = LayoutParams(5.dp, ViewGroup.LayoutParams.MATCH_PARENT)
      enableRoundCorner = GlobalValues.md3Theme
      addView(this)
    }

    private var redMask: View? = null

    fun addRedMask() {
      if (redMask == null) {
        redMask = View(context).apply {
          layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          setBackgroundColor(R.color.material_red_300.getColor(context))
          alpha = 0.5f
          addView(this)
        }
      }
    }

    fun removeRedMask() {
      if (redMask != null) {
        removeView(redMask)
        redMask = null
      }
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
      abiInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        abiInfo.defaultHeightMeasureSpec(this)
      )

      val packageSizeHeight = if (packageSizeInfo.isVisible) {
        packageSizeInfo.measuredHeight
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
            abiInfo.measuredHeight +
            paddingBottom
          )
      )
      stateIndicator.measure(
        stateIndicator.defaultWidthMeasureSpec(this),
        (measuredHeight - paddingTop - paddingBottom).toExactlyMeasureSpec()
      )
      redMask?.autoMeasure()
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
      abiInfo.layout(appName.left, targetApiInfo.bottom)
      stateIndicator.layout(
        paddingEnd,
        stateIndicator.toVerticalCenter(this),
        fromRight = true
      )
      redMask?.layout(0, 0)
    }
  }
}
