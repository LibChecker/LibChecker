package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotTitlePackageSizeRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotTitleRenderState
import com.absinthe.libchecker.domain.snapshot.ui.view.SnapshotPackageSizeLineBreaker
import com.absinthe.libchecker.utils.extensions.applyCondensedSingleLine
import com.absinthe.libchecker.utils.extensions.applySingleLineEndEllipsize
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AlwaysMarqueeTextView

class SnapshotTitleView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  val iconView = AppCompatImageView(context).apply {
    val iconSize = context.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    layoutParams = LayoutParams(iconSize, iconSize)
    setImageResource(R.drawable.ic_icon_blueprint)
    addView(this)
  }

  val appNameView = AlwaysMarqueeTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = context.getDimensionPixelSize(R.dimen.normal_padding)
    }
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleMedium))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    addView(this)
  }

  val packageNameView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    applySingleLineEndEllipsize()
    addView(this)
  }

  val versionInfoView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    applyCondensedSingleLine()
    addView(this)
  }

  val packageSizeView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    applyCondensedSingleLine()
    setSingleLine(false)
    setHorizontallyScrolling(false)
    maxLines = Int.MAX_VALUE
    ellipsize = null
    addView(this)
  }
  private val packageSizeLineBreaker = SnapshotPackageSizeLineBreaker(packageSizeView)

  val apisView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    applyCondensedSingleLine()
    addView(this)
  }

  fun render(data: SnapshotTitleRenderState) {
    appNameView.apply {
      text = data.appName
      if (data.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    iconView.contentDescription = data.iconContentDescription
    packageNameView.apply {
      text = data.packageName
      if (data.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    versionInfoView.apply {
      text = data.versionInfo
      if (data.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    setPackageSizeText(data.packageSize)
    apisView.apply {
      text = data.apis
      setLongClickCopiedToClipboard(text)
    }
  }

  private fun setPackageSizeText(data: SnapshotTitlePackageSizeRenderState?) {
    if (data == null) {
      packageSizeView.isVisible = false
      packageSizeLineBreaker.clear()
      return
    }

    packageSizeView.apply {
      isVisible = true
      packageSizeLineBreaker.setText(data.text, data.breakStart)
      setLongClickCopiedToClipboard(text)
    }
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
    if (packageSizeView.isVisible) {
      packageSizeLineBreaker.apply(textWidth)
      packageSizeView.measure(
        textWidth.toExactlyMeasureSpec(),
        packageSizeView.defaultHeightMeasureSpec(this)
      )
    }
    if (apisView.measuredWidth > textWidth) {
      apisView.measure(
        textWidth.toExactlyMeasureSpec(),
        apisView.defaultHeightMeasureSpec(this)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        appNameView.measuredHeight +
        packageNameView.measuredHeight +
        versionInfoView.measuredHeight +
        packageSizeView.measuredHeight +
        apisView.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    iconView.layout(paddingStart, paddingTop)
    val appNameXOffset = paddingStart + iconView.measuredWidth + appNameView.marginStart
    appNameView.layout(appNameXOffset, paddingTop)
    packageNameView.layout(appNameXOffset, appNameView.bottom)
    versionInfoView.layout(appNameXOffset, packageNameView.bottom)
    packageSizeView.layout(appNameXOffset, versionInfoView.bottom)
    apisView.layout(appNameXOffset, packageSizeView.bottom)
  }
}
