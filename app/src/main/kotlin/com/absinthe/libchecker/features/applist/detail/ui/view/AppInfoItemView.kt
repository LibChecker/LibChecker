package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.utils.extensions.toColorStateListByColor

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoItemView(context: Context) : LinearLayout(context) {

  init {
    orientation = VERTICAL
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    gravity = Gravity.CENTER_HORIZONTAL
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
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    gravity = Gravity.CENTER_HORIZONTAL
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    setPadding(0, 4.dp, 0, 4.dp)
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
}
