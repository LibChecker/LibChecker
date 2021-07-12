package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionByAttr
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class SnapshotDetailTitleView(context: Context) : AViewGroup(context) {

    val arrow = ImageView(context).apply {
        layoutParams = LayoutParams(24.dp, 24.dp).also {
            it.marginStart = 16.dp
        }
        setImageResource(R.drawable.ic_arrow)
        addView(this)
    }

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(10.dp, 6.dp, 10.dp, 6.dp)
        text = context.getString(R.string.snapshot_empty_list_title)
        setTextAppearance(context.getResourceIdByAttr(android.R.attr.textAppearanceListItemSmall))
        setTypeface(null, Typeface.BOLD)
        addView(this)
    }

    val list = RecyclerView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    init {
        background = context.getDrawableByAttr(android.R.attr.selectableItemBackground)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.getDimensionByAttr(android.R.attr.listPreferredItemHeightSmall).toInt())
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        arrow.autoMeasure()
        title.autoMeasure()
        list.autoMeasure()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        arrow.layout(arrow.marginStart, arrow.toVerticalCenter(this))
        title.layout(arrow.right, title.toVerticalCenter(this))
        list.layout(title.right, list.toVerticalCenter(this))
    }
}
