package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.app.IHeaderView

class AppBundleBottomSheetView(context: Context) : AViewGroup(context), IHeaderView {

    private val header = BottomSheetHeaderView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        title.text = context.getString(R.string.app_bundle)
    }

    val list = RecyclerView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = 16.dp
        }
        overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }

    init {
        addView(header)
        addView(list)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        header.autoMeasure()
        list.measure(
            (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
            list.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            paddingTop + header.measuredHeight + list.marginTop + list.measuredHeight + paddingBottom
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        header.layout(0, paddingTop)
        list.layout(paddingStart, header.bottom + list.marginTop)
    }

    override fun getHeaderView(): BottomSheetHeaderView {
        return header
    }
}