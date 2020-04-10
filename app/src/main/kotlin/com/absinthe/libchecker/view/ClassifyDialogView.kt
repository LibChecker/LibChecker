package com.absinthe.libchecker.view

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.viewholder.AppItemViewBinder
import com.drakeet.multitype.MultiTypeAdapter

class ClassifyDialogView(context: Context) : LinearLayout(context) {

    var adapter = MultiTypeAdapter()

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        adapter.register(AppItemViewBinder())

        val rvList = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
            adapter = this@ClassifyDialogView.adapter
        }

        addView(rvList)
    }

}