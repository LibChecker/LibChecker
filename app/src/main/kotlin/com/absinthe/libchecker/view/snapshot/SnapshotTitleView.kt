package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AlwaysMarqueeTextView

class SnapshotTitleView(context: Context, attributeSet: AttributeSet? = null) :
  AViewGroup(context, attributeSet) {

  val iconView = AppCompatImageView(context).apply {
    val iconSize = context.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    layoutParams = LayoutParams(iconSize, iconSize)
    setImageResource(R.drawable.ic_icon_blueprint)
    addView(this)
  }

  val appNameView = AlwaysMarqueeTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = context.getDimensionPixelSize(R.dimen.normal_padding)
      it.marginEnd = context.getDimensionPixelSize(R.dimen.normal_padding)
    }
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    addView(this)
  }

  val packageNameView =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  val versionInfoView = AppCompatTextView(
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
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    addView(this)
  }

  val packageSizeView = AppCompatTextView(
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
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    addView(this)
  }

  val targetApiView = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextColor(android.R.color.darker_gray.getColor(context))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    iconView.autoMeasure()
    val textWidth =
      measuredWidth - paddingStart - paddingEnd - iconView.measuredWidth - appNameView.marginStart
    targetApiView.autoMeasure()
    appNameView.measure(
      (textWidth - targetApiView.measuredWidth - appNameView.marginEnd).toExactlyMeasureSpec(),
      appNameView.defaultHeightMeasureSpec(this)
    )
    packageNameView.measure(
      textWidth.toExactlyMeasureSpec(),
      packageNameView.defaultHeightMeasureSpec(this)
    )
    versionInfoView.measure(
      textWidth.toExactlyMeasureSpec(),
      versionInfoView.defaultHeightMeasureSpec(this)
    )
    packageSizeView.measure(
      textWidth.toExactlyMeasureSpec(),
      packageSizeView.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        appNameView.measuredHeight +
        packageNameView.measuredHeight +
        versionInfoView.measuredHeight +
        packageSizeView.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    iconView.layout(paddingStart, paddingTop)
    appNameView.layout(iconView.right + appNameView.marginStart, paddingTop)
    packageNameView.layout(appNameView.left, appNameView.bottom)
    versionInfoView.layout(appNameView.left, packageNameView.bottom)
    packageSizeView.layout(appNameView.left, versionInfoView.bottom)
    targetApiView.layout(paddingEnd, paddingTop, fromRight = true)
  }
}
