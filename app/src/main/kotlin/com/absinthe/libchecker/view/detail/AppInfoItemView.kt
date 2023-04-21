package com.absinthe.libchecker.view.detail

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.utils.extensions.toColorStateListByColor
import com.absinthe.libchecker.view.AViewGroup

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoItemView(context: Context) : AViewGroup(context) {

  init {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    isClickable = true
    isFocusable = true
    setPadding(4.dp, 12.dp, 4.dp, 12.dp)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
  }

  private val icon = AppCompatImageView(context).apply {
    val iconSize = context.getDimensionPixelSize(R.dimen.app_info_icon_size)
    layoutParams = LayoutParams(iconSize, iconSize)
    scaleType = ImageView.ScaleType.CENTER_CROP
    setBackgroundResource(R.drawable.bg_circle_secondary_container)
    addView(this)
  }

  private val text = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 12.dp
    }
    gravity = Gravity.CENTER_HORIZONTAL
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    addView(this)
  }

  fun setText(charSequence: CharSequence) {
    text.text = charSequence
    text.isVisible = charSequence.isNotEmpty()
  }

  fun setText(@StringRes res: Int) {
    setText(context.getString(res))
  }

  fun setIcon(@DrawableRes res: Int) {
    icon.setImageResource(res)
  }

  fun setIcon(drawable: Drawable, cleanBackground: Boolean) {
    icon.setImageDrawable(drawable)
    if (cleanBackground) {
      icon.background = null
    } else {
      icon.setBackgroundResource(R.drawable.bg_circle_secondary_container)
    }
  }

  fun setIconBackground(drawable: Drawable?) {
    icon.background = drawable
  }

  fun setIconTintColorResource(@ColorRes res: Int) {
    icon.imageTintList = res.toColorStateList(context)
  }

  fun setIconTintColor(@ColorInt res: Int) {
    icon.imageTintList = res.toColorStateListByColor()
  }

  fun setIconBackgroundTintColor(@ColorRes res: Int) {
    icon.backgroundTintList = res.toColorStateList(context)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    icon.autoMeasure()
    text.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      text.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      // Ignore errors within 10 pixels, as the different heights of Chinese and English in some fonts can cause the Adapter to fail to align
      (paddingTop + icon.measuredHeight + text.marginTop + text.measuredHeight + paddingBottom) / 10 * 10
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    icon.layout(icon.toHorizontalCenter(this), paddingTop)
    text.layout(text.toHorizontalCenter(this), icon.bottom + text.marginTop)
  }
}
