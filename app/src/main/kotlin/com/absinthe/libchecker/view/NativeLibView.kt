package com.absinthe.libchecker.view

import android.content.Context
import android.content.res.Resources
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.viewholder.LibStringItemViewBinder
import com.drakeet.multitype.MultiTypeAdapter

class NativeLibView(context: Context) : LinearLayout(context) {

    val Number.dp: Int get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()

    var adapter = MultiTypeAdapter()

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(25.dp, 20.dp, 25.dp, 20.dp)

        adapter.register(LibStringItemViewBinder())

        val rvList = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
            adapter = this@NativeLibView.adapter
        }

        addView(rvList)
    }

}