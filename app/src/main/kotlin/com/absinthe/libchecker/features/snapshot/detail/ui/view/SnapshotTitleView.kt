package com.absinthe.libchecker.features.snapshot.detail.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.sizeToString
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

  val packageSizeView = AppCompatTextView(
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

  val apisView = AppCompatTextView(
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

  fun setPackageSizeText(item: SnapshotDiffItem, isNewOrDeleted: Boolean) {
    if (item.packageSizeDiff.old > 0L) {
      packageSizeView.isVisible = true
      val sizeDiff = SnapshotDiffItem.DiffNode(
        item.packageSizeDiff.old.sizeToString(context),
        item.packageSizeDiff.new?.sizeToString(context)
      )
      val sizeDiffAppend = StringBuilder(LCAppUtils.getDiffString(sizeDiff, isNewOrDeleted))
      if (item.packageSizeDiff.new != null) {
        val diffSize = item.packageSizeDiff.new - item.packageSizeDiff.old
        val diffSizeText = buildString {
          append(if (diffSize > 0) "+" else "")
          append(diffSize.sizeToString(context))
        }

        if (diffSize != 0L) {
          sizeDiffAppend.append(", $diffSizeText")
        }
      }
      packageSizeView.text = sizeDiffAppend
    } else {
      packageSizeView.isVisible = false
    }
  }

  fun setApisText(item: SnapshotDiffItem, isNewOrDeleted: Boolean) {
    val targetDiff = LCAppUtils.getDiffString(item.targetApiDiff, isNewOrDeleted).takeIf { item.targetApiDiff.old > 0 }
    val minDiff = LCAppUtils.getDiffString(item.minSdkDiff, isNewOrDeleted).takeIf { item.minSdkDiff.old > 0 }
    val compileDiff = LCAppUtils.getDiffString(item.compileSdkDiff, isNewOrDeleted).takeIf { item.compileSdkDiff.old > 0 }
    apisView.text = buildSpannedString {
      targetDiff?.let {
        scale(1f) {
          append("Target: ")
        }
        append(it)
        append("  ")
      }

      minDiff?.let {
        scale(1f) {
          append("Min: ")
        }
        append(it)
        append("  ")
      }

      compileDiff?.let {
        scale(1f) {
          append("Compile: ")
        }
        append(it)
      }
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
        (textWidth - apisView.measuredWidth - appNameView.marginEnd).toExactlyMeasureSpec(),
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
    if (packageSizeView.measuredWidth > textWidth) {
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
