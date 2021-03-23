package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class TrackItemView(context: Context) : MaterialCardView(context) {

    val container = TrackItemContainerView(context).apply {
        val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
        setPadding(padding, padding, padding, padding)
    }

    init {
        addView(container)
    }

    class TrackItemContainerView(context: Context) : AViewGroup(context) {

        val icon = AppCompatImageView(context).apply {
            val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
            addView(this)
        }

        val appName = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 8.dp
            }
            setTextColor(ContextCompat.getColor(context, R.color.textNormal))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            addView(this)
        }

        val packageName = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextColor(ContextCompat.getColor(context, R.color.textNormal))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            addView(this)
        }

        val switch = SwitchMaterial(context).apply {
            id = android.R.id.toggle
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(this)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            switch.autoMeasure()
            val textWidth = measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - appName.marginStart - switch.measuredWidth
            appName.measure(textWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this))
            packageName.measure(textWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this))
            setMeasuredDimension(measuredWidth, (paddingTop + appName.measuredHeight + packageName.measuredHeight + paddingBottom)
                .coerceAtLeast(icon.measuredHeight + paddingTop + paddingBottom))
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            icon.layout(paddingStart, icon.toVerticalCenter(this))
            switch.layout(paddingEnd, switch.toVerticalCenter(this), fromRight = true)
            appName.layout(icon.right + appName.marginStart, paddingTop)
            packageName.layout(appName.left, appName.bottom)
        }
    }
}