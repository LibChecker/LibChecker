package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.TimeNodeAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy

class TimeNodeBottomSheetView(context: Context) : LinearLayout(context) {

    val adapter by unsafeLazy { TimeNodeAdapter() }

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(24.dp, 16.dp, 24.dp, 0)
        val list = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            adapter = this@TimeNodeBottomSheetView.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpacesItemDecoration(4.dp))
        }
        addView(list)
    }
}
