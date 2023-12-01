package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.view.AViewGroup
import rikka.core.util.ClipboardUtils

class SignatureDetailItemView(context: Context) : AViewGroup(context) {

  val type = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }

  val content = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
    }
  }

  private val copyToClipboard = AppCompatImageButton(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp).also {
      it.marginStart = 8.dp
    }
    scaleX = 0.8F
    scaleY = 0.8F
    setImageResource(com.google.android.material.R.drawable.abc_ic_menu_copy_mtrl_am_alpha)
    setBackgroundDrawable(context.getDrawableByAttr(com.google.android.material.R.attr.selectableItemBackgroundBorderless))
    setOnClickListener {
      ClipboardUtils.put(
        context,
        "${type.text}:${content.text}"
      )
      VersionCompat.showCopiedOnClipboardToast(context)
    }
  }

  init {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setPadding(8.dp, 2.dp, 8.dp, 2.dp)
    addView(type)
    addView(copyToClipboard)
    addView(content)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val typeWidth =
      measuredWidth - paddingStart - paddingEnd - copyToClipboard.measuredWidth - copyToClipboard.marginStart
    val contentWidth = measuredWidth - paddingStart - paddingEnd
    if (type.measuredWidth > typeWidth) {
      type.measure(typeWidth.toExactlyMeasureSpec(), type.defaultHeightMeasureSpec(this))
    }
    if (content.measuredWidth > contentWidth) {
      content.measure(contentWidth.toExactlyMeasureSpec(), content.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      (type.measuredHeight + content.measuredHeight + paddingTop + paddingBottom).coerceAtLeast(
        40.dp
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    type.layout(paddingStart, paddingTop)
    content.layout(paddingStart, type.bottom)
    copyToClipboard.layout(paddingEnd, copyToClipboard.toViewVerticalCenter(type), true)
  }
}
