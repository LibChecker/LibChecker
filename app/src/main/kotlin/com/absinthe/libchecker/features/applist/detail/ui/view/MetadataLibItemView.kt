package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.utils.extensions.visibleWidth
import com.absinthe.libchecker.view.AViewGroup

class MetadataLibItemView(context: Context) : AViewGroup(context) {

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    val verticalPadding = 4.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
  }

  val libName =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginEnd = context.getDimensionPixelSize(R.dimen.normal_padding)
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  val libSize =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      addView(this)
    }

  val linkToIcon = AppCompatImageButton(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp).also {
      it.marginStart = 8.dp
    }
    scaleType = ImageView.ScaleType.CENTER
    setImageResource(R.drawable.ic_outline_change_circle_24)
    setBackgroundDrawable(context.getDrawableByAttr(com.google.android.material.R.attr.selectableItemBackgroundBorderless))
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val libNameWidth =
      measuredWidth - paddingStart - paddingEnd - libName.marginEnd - linkToIcon.visibleWidth() - linkToIcon.marginStart
    if (libName.measuredWidth > libNameWidth) {
      libName.measure(libNameWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    }
    if (libSize.measuredWidth > libNameWidth) {
      libSize.measure(libNameWidth.toExactlyMeasureSpec(), libSize.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      (libName.measuredHeight + libSize.measuredHeight + paddingTop + paddingBottom).coerceAtLeast(
        40.dp
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    libName.layout(paddingStart, paddingTop)
    libSize.layout(paddingStart, libName.bottom)
    linkToIcon.layout(paddingEnd, linkToIcon.toVerticalCenter(this), true)
  }
}
