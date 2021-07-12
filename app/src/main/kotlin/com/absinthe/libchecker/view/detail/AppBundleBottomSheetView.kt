package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.detail.AppBundleAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.unsafeLazy

class AppBundleBottomSheetView(context: Context) : LinearLayout(context) {

    val adapter by unsafeLazy { AppBundleAdapter() }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(24.dp, 16.dp, 24.dp, 0)
        val list = RecyclerView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            adapter = this@AppBundleBottomSheetView.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpacesItemDecoration(4.dp))
        }
        addView(list)
    }
}
