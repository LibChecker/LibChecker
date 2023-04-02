package com.absinthe.libchecker.view.detail

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.detail.AbiLabelsAdapter
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
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

  val abiLabelsAdapter = AbiLabelsAdapter()

  val abiLabelsRecyclerView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
      it.marginStart = (-4).dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = abiLabelsAdapter
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    isHorizontalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    setHasFixedSize(true)
    addItemDecoration(HorizontalSpacesItemDecoration(4.dp))
    this@DetailsTitleView.addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    iconView.autoMeasure()
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
    abiLabelsRecyclerView.measure(
      textWidth.toExactlyMeasureSpec(),
      abiLabelsRecyclerView.defaultHeightMeasureSpec(this)
    )
    val basicInfoTotalHeight =
      appNameView.measuredHeight +
        packageNameView.measuredHeight +
        versionInfoView.measuredHeight +
        extraInfoView.measuredHeight +
        abiLabelsRecyclerView.measuredHeight +
        abiLabelsRecyclerView.marginTop
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        (basicInfoTotalHeight).coerceAtLeast(iconView.measuredHeight) +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    iconView.layout(paddingStart, paddingTop)
    appNameView.layout(iconView.right + appNameView.marginStart, paddingTop)
    packageNameView.layout(appNameView.left, appNameView.bottom)
    versionInfoView.layout(appNameView.left, packageNameView.bottom)
    extraInfoView.layout(appNameView.left, versionInfoView.bottom)
    abiLabelsRecyclerView.layout(appNameView.left + abiLabelsRecyclerView.marginStart, extraInfoView.bottom + abiLabelsRecyclerView.marginTop)
  }

  companion object {
    private const val MEGA_BYTE_SI_UNITS = 1000 * 1000
  }
}
