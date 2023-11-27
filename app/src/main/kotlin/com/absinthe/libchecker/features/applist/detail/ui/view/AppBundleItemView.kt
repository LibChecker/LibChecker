package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup

class AppBundleItemView(context: Context) : AViewGroup(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp)
  }

  private val name =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
    }

  private val size = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    addView(icon)
    addView(name)
    addView(size)
  }

  fun setIcon(type: Int) {
    when (type) {
      IconType.TYPE_NATIVE_LIBS -> icon.setImageResource(R.drawable.ic_logo)
      IconType.TYPE_MATERIALS -> icon.setImageResource(R.drawable.ic_outline_image)
      IconType.TYPE_STRINGS -> icon.setImageResource(R.drawable.ic_translate)
      IconType.TYPE_OTHERS -> icon.setImageResource(R.drawable.ic_split)
      else -> throw IllegalArgumentException("wrong type")
    }
  }

  fun setNameText(text: String) {
    name.text = text
  }

  fun setSizeText(text: String) {
    size.text = text
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val textWidth =
      measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - name.marginStart
    if (name.measuredWidth > textWidth) {
      name.measure(textWidth.toExactlyMeasureSpec(), name.defaultHeightMeasureSpec(this))
    }
    if (size.measuredWidth > textWidth) {
      size.measure(textWidth.toExactlyMeasureSpec(), size.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + name.measuredHeight + size.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    icon.layout(paddingStart, icon.toVerticalCenter(this))
    name.layout(paddingStart + icon.measuredWidth + name.marginStart, paddingTop)
    size.layout(paddingStart + icon.measuredWidth + name.marginStart, name.bottom)
  }

  class IconType {
    companion object {
      const val TYPE_NATIVE_LIBS = 0
      const val TYPE_MATERIALS = 1
      const val TYPE_STRINGS = 2
      const val TYPE_OTHERS = 3
    }
  }
}
