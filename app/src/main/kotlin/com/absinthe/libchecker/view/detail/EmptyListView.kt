package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class EmptyListView(context: Context) : AViewGroup(context) {

    val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.empty_list)
        setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline5))
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        text.autoMeasure()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        text.layout(text.toHorizontalCenter(this), measuredWidth / 2)
    }
}