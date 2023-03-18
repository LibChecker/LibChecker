package com.absinthe.libchecker.view.detail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.format.Formatter
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AlwaysMarqueeTextView

class DetailsTitleView(context: Context, attributeSet: AttributeSet? = null) :
  AViewGroup(context, attributeSet) {

  val iconView = AppCompatImageView(context).apply {
    val iconSize = context.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    layoutParams = LayoutParams(iconSize, iconSize)
    addView(this)
  }

  private val appSizeView = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
    }
    setPadding(4.dp, 2.dp, 4.dp, 2.dp)
    setBackgroundResource(R.drawable.bg_app_size_label)
    setTextColor(android.R.color.darker_gray.getColor(context))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    setTypeface(null, Typeface.BOLD)
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

  val extraInfoView = AppCompatTextView(
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
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    addView(this)
  }

  fun setAppSize(size: Long) {
    val tint: ColorStateList = if (size <= 20 * MEGA_BYTE_SI_UNITS) {
      context.getColorStateList(R.color.material_green_700)
    } else if (size <= 50 * MEGA_BYTE_SI_UNITS) {
      context.getColorStateList(R.color.material_blue_700)
    } else if (size <= 100 * MEGA_BYTE_SI_UNITS) {
      context.getColorStateList(R.color.material_yellow_700)
    } else if (size <= 150 * MEGA_BYTE_SI_UNITS) {
      context.getColorStateList(R.color.material_red_700)
    } else {
      context.getColorStateListByAttr(com.google.android.material.R.attr.colorOnSurface)
    }
    appSizeView.backgroundTintList = tint
    appSizeView.setTextColor(tint)

    appSizeView.text = Formatter.formatFileSize(context, size)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    iconView.autoMeasure()
    appSizeView.autoMeasure()
    val textWidth =
      measuredWidth - paddingStart - paddingEnd - iconView.measuredWidth - appNameView.marginStart
    appNameView.measure(
      textWidth.toExactlyMeasureSpec(),
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
    extraInfoView.measure(
      textWidth.toExactlyMeasureSpec(),
      extraInfoView.defaultHeightMeasureSpec(this)
    )
    val basicInfoTotalHeight =
      appNameView.measuredHeight + packageNameView.measuredHeight + versionInfoView.measuredHeight + extraInfoView.measuredHeight
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        (basicInfoTotalHeight).coerceAtLeast(iconView.measuredHeight + appSizeView.marginTop + appSizeView.measuredHeight) +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    iconView.layout(paddingStart, paddingTop)
    appSizeView.layout(iconView.left + (iconView.measuredWidth - appSizeView.measuredWidth) / 2, iconView.bottom + appSizeView.marginTop)
    appNameView.layout(iconView.right + appNameView.marginStart, paddingTop)
    packageNameView.layout(appNameView.left, appNameView.bottom)
    versionInfoView.layout(appNameView.left, packageNameView.bottom)
    extraInfoView.layout(appNameView.left, versionInfoView.bottom)
  }

  companion object {
    private const val MEGA_BYTE_SI_UNITS = 1000 * 1000
  }
}
