package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class DexListEmptyView(context: Context) : AViewGroup(context) {

  private val image = ImageView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.lottie_anim_size)
    layoutParams = LayoutParams(size, size)
    setImageResource(R.drawable.ic_dex_empty_list_placeholder)
    addView(this)
  }

  val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    text = context.getString(R.string.loading)
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline5))
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    image.autoMeasure()
    text.autoMeasure()
    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    image.layout(image.toHorizontalCenter(this), measuredWidth / 4)
    text.layout(text.toHorizontalCenter(this), image.bottom)
  }
}
