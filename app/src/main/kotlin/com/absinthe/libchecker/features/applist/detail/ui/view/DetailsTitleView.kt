package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.AbiLabelNode
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AlwaysMarqueeTextView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class DetailsTitleView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

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
      ViewGroup.LayoutParams.WRAP_CONTENT,
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
        ViewGroup.LayoutParams.WRAP_CONTENT,
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
      ViewGroup.LayoutParams.WRAP_CONTENT,
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
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextColor(android.R.color.darker_gray.getColor(context))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    addView(this)
  }

  fun setAbiLabels(abis: List<AbiLabelNode>) {
    abis.forEach {
      val res = when (it.abi) {
        Constants.ARMV8 -> R.drawable.ic_abi_label_arm64_v8a
        Constants.ARMV7 -> R.drawable.ic_abi_label_armeabi_v7a
        Constants.ARMV5 -> R.drawable.ic_abi_label_armeabi
        Constants.X86_64 -> R.drawable.ic_abi_label_x86_64
        Constants.X86 -> R.drawable.ic_abi_label_x86
        Constants.MIPS64 -> R.drawable.ic_abi_label_mips64
        Constants.MIPS -> R.drawable.ic_abi_label_mips
        Constants.RISCV64 -> R.drawable.ic_abi_label_riscv64
        Constants.RISCV32 -> R.drawable.ic_abi_label_riscv32
        Constants.MULTI_ARCH -> R.drawable.ic_abi_label_multi_arch
        else -> throw IllegalArgumentException("wrong abi label")
      }
      val view = ImageView(context).also { v ->
        v.layoutParams = MarginLayoutParams(42.dp, 28.dp).apply {
          marginEnd = 4.dp
          bottomMargin = 4.dp
        }
        v.scaleType = ImageView.ScaleType.CENTER_CROP
        v.setImageResource(res)
        v.alpha = if (it.active) 1f else 0.5f
        it.action?.let { action ->
          v.setOnClickListener { action.invoke() }
        }
      }
      abiLabelsFlexLayout.addView(view)
    }
  }

  private val abiLabelsFlexLayout = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
      it.marginStart = (-4).dp
      it.bottomMargin = (-4).dp
    }
    overScrollMode = OVER_SCROLL_NEVER
    isHorizontalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    flexWrap = FlexWrap.WRAP
    justifyContent = JustifyContent.FLEX_START
    flexDirection = FlexDirection.ROW
    this@DetailsTitleView.addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val textWidth =
      measuredWidth - paddingStart - paddingEnd - iconView.measuredWidth - appNameView.marginStart
    if (appNameView.measuredWidth > textWidth) {
      appNameView.measure(
        textWidth.toExactlyMeasureSpec(),
        appNameView.defaultHeightMeasureSpec(this)
      )
    }
    if (packageNameView.measuredWidth > textWidth) {
      packageNameView.measure(
        textWidth.toExactlyMeasureSpec(),
        packageNameView.defaultHeightMeasureSpec(this)
      )
    }
    if (versionInfoView.measuredWidth > textWidth) {
      versionInfoView.measure(
        textWidth.toExactlyMeasureSpec(),
        versionInfoView.defaultHeightMeasureSpec(this)
      )
    }
    if (extraInfoView.measuredWidth > textWidth) {
      extraInfoView.measure(
        textWidth.toExactlyMeasureSpec(),
        extraInfoView.defaultHeightMeasureSpec(this)
      )
    }
    if (abiLabelsFlexLayout.measuredWidth > textWidth) {
      abiLabelsFlexLayout.measure(
        textWidth.toExactlyMeasureSpec(),
        abiLabelsFlexLayout.defaultHeightMeasureSpec(this)
      )
    }
    val basicInfoTotalHeight =
      appNameView.measuredHeight +
        packageNameView.measuredHeight +
        versionInfoView.measuredHeight +
        extraInfoView.measuredHeight +
        abiLabelsFlexLayout.measuredHeight +
        abiLabelsFlexLayout.marginTop
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        (basicInfoTotalHeight).coerceAtLeast(iconView.measuredHeight) +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    iconView.layout(paddingStart, paddingTop)
    val appNameXOffset = paddingStart + iconView.measuredWidth + appNameView.marginStart
    appNameView.layout(appNameXOffset, paddingTop)
    packageNameView.layout(appNameXOffset, appNameView.bottom)
    versionInfoView.layout(appNameXOffset, packageNameView.bottom)
    extraInfoView.layout(appNameXOffset, versionInfoView.bottom)
    abiLabelsFlexLayout.layout(appNameXOffset + abiLabelsFlexLayout.marginStart, extraInfoView.bottom + abiLabelsFlexLayout.marginTop)
  }
}
