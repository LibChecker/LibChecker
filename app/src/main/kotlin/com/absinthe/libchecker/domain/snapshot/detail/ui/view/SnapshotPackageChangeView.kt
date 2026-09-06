package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class SnapshotPackageChangeView(
  context: Context,
  @DrawableRes illustration: Int,
  @StringRes message: Int
) : AViewGroup(context) {

  private val image = ImageView(context).apply {
    layoutParams = LayoutParams(150.dp, 150.dp)
    setImageResource(illustration)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val text =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.topMargin = 16.dp
      }
      text = context.getString(message)
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleLarge))
      addView(this)
    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setPadding(0, measuredWidth / 8, 0, measuredWidth / 8)
    image.autoMeasure()
    text.autoMeasure()
    setMeasuredDimension(
      measuredWidth,
      paddingTop + image.measuredHeight + text.marginTop + text.measuredHeight + paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    image.layout(image.toHorizontalCenter(this), paddingTop)
    text.layout(text.toHorizontalCenter(this), image.bottom + text.marginTop)
  }
}
