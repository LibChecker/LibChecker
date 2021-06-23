package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class SnapshotEmptyView(context: Context) : AViewGroup(context) {

    private val image = ImageView(context).apply {
        layoutParams = LayoutParams(200.dp, 200.dp)
        setImageResource(R.drawable.ic_natural_food)
        addView(this)
    }

    val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = 16.dp
        }
        text = context.getString(R.string.snapshot_empty_list_title)
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
        image.layout(image.toHorizontalCenter(this), image.toVerticalCenter(this))
        text.layout(text.toHorizontalCenter(this), image.bottom + text.marginTop)
    }
}