package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/22
 * </pre>
 */
class AlbumItemView(context: Context) : MaterialCardView(context) {

    val container = AlbumItemContainerView(context).apply {
        val paddingHorizontal = context.getDimensionPixelSize(R.dimen.album_card_inset_horizontal)
        val paddingVertical = context.getDimensionPixelSize(R.dimen.album_card_inset_vertical)
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
    }

    init {
        addView(container)
    }

    class AlbumItemContainerView(context: Context) : AViewGroup(context) {

        private val icon = AppCompatImageView(context).apply {
            val iconSize = context.getDimensionPixelSize(R.dimen.album_card_icon_size)
            layoutParams = LayoutParams(iconSize, iconSize)
            setBackgroundResource(R.drawable.bg_gray_circle)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val title = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = context.getDimensionPixelSize(R.dimen.album_card_inset_horizontal)
            }
            setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline6))
        }

        val subtitle = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
            setTextColor(context.getColor(R.color.textSecondary))
        }

        init {
            background = null
            addView(icon)
            addView(title)
            addView(subtitle)
        }

        fun setIcon(@DrawableRes res: Int) {
            icon.setImageResource(res)
        }

        fun setIconBackgroundColor(@ColorRes res: Int) {
            icon.backgroundTintList = ColorStateList.valueOf(context.getColor(res))
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            val textWidth = measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - title.marginStart
            title.measure(textWidth.toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
            subtitle.measure(textWidth.toExactlyMeasureSpec(), subtitle.defaultHeightMeasureSpec(this))
            setMeasuredDimension(measuredWidth, (title.measuredHeight + subtitle.measuredHeight).coerceAtLeast(icon.measuredHeight) + paddingTop + paddingBottom)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            icon.layout(paddingStart, icon.toVerticalCenter(this))
            title.layout(icon.right + title.marginStart, (measuredHeight - title.measuredHeight - subtitle.measuredHeight) / 2)
            subtitle.layout(title.left, title.bottom)
        }
    }

}
