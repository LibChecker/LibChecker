package com.absinthe.libchecker.view.detail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
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
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
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
        setPadding(8.dp, 24.dp, 8.dp, 0)
        setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
    }

    private val icon = AppCompatImageView(context).apply {
        val iconSize = context.getDimensionPixelSize(R.dimen.app_info_icon_size)
        layoutParams = LayoutParams(iconSize, iconSize)
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundResource(R.drawable.bg_gray_circle)
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
        setLines(2)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        addView(this)
    }

    fun setText(charSequence: CharSequence) {
        text.text = charSequence
    }

    fun setText(@StringRes res: Int) {
        text.text = context.getString(res)
    }

    fun setIcon(@DrawableRes res: Int) {
        icon.setImageResource(res)
    }

    fun setIcon(drawable: Drawable, cleanBackground: Boolean) {
        icon.setImageDrawable(drawable)
        if (cleanBackground) {
            icon.background = null
        } else {
            icon.setBackgroundResource(R.drawable.bg_gray_circle)
        }
    }

    fun setIconBackground(drawable: Drawable?) {
        icon.background = drawable
    }

    fun setIconTintColorResource(@ColorRes res: Int) {
        icon.imageTintList = ColorStateList.valueOf(context.getColor(res))
    }

    fun setIconTintColor(@ColorInt res: Int) {
        icon.imageTintList = ColorStateList.valueOf(res)
    }

    fun setIconBackgroundTintColor(@ColorRes res: Int) {
        icon.backgroundTintList = ColorStateList.valueOf(context.getColor(res))
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
            (paddingTop + icon.measuredHeight + text.marginTop + text.measuredHeight + paddingBottom).coerceAtLeast(
                135.dp
            )
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        icon.layout(icon.toHorizontalCenter(this), paddingTop)
        text.layout(text.toHorizontalCenter(this), icon.bottom + text.marginTop)
    }
}
