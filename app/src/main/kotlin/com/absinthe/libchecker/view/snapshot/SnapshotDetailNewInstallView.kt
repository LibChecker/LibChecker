package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class SnapshotDetailNewInstallView(context: Context) : AViewGroup(context) {

    private val image = ImageView(context).apply {
        layoutParams = LayoutParams(150.dp, 150.dp)
        setImageResource(R.drawable.ic_yes)
        addView(this)
    }

    private val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = 16.dp
        }
        text = context.getString(R.string.snapshot_detail_new_install_title)
        setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline6))
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        image.autoMeasure()
        text.autoMeasure()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        image.layout(image.toHorizontalCenter(this), measuredWidth / 2)
        text.layout(text.toHorizontalCenter(this), image.bottom + text.marginTop)
    }
}
