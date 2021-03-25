package com.absinthe.libchecker.view.statistics

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.absinthe.libchecker.R
import com.absinthe.libchecker.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class LibReferenceItemView(context: Context) : MaterialCardView(context) {

    val container = LibReferenceItemContainerView(context).apply {
        val padding = R.dimen.main_card_padding.px
        setPadding(padding, padding, padding, padding)
    }

    init {
        addView(container)
    }

    class LibReferenceItemContainerView(context: Context) : AViewGroup(context) {

        val icon = AppCompatImageButton(context).apply {
            id = android.R.id.icon
            val iconSize = R.dimen.lib_reference_icon_size.px
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
            setBackgroundResource(R.drawable.bg_gray_circle)
            addView(this)
        }

        val labelName = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 8.dp
                it.marginEnd = 8.dp
            }
            setTextColor(ContextCompat.getColor(context, R.color.textNormal))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            addView(this)
        }

        val libName = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 8.dp
                it.marginEnd = 8.dp
            }
            setTextColor(ContextCompat.getColor(context, R.color.textNormal))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            addView(this)
        }

        val count = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline4))
            addView(this)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            count.autoMeasure()
            val labelWidth = (measuredWidth - paddingStart - paddingEnd
            - icon.measuredWidth - count.measuredWidth
            - labelName.marginLeft - labelName.marginRight)
            labelName.measure(labelWidth.toExactlyMeasureSpec(), labelName.defaultHeightMeasureSpec(this))
            libName.measure(labelWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
            setMeasuredDimension(measuredWidth, (paddingTop + labelName.measuredHeight + libName.measuredHeight + paddingBottom)
                .coerceAtLeast(icon.measuredHeight + paddingTop + paddingBottom))
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            icon.layout(paddingStart, icon.toVerticalCenter(this))
            labelName.layout(icon.right + labelName.marginLeft, paddingTop)
            libName.layout(labelName.left, labelName.bottom)
            count.layout(paddingEnd, count.toVerticalCenter(this), fromRight = true)
        }
    }
}