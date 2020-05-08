package com.absinthe.libchecker.view

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.recyclerview.MODE_NATIVE

const val MODE_SOR_BY_SIZE = 0
const val MODE_SOR_BY_LIB = 1

class NativeLibView(context: Context) : ConstraintLayout(context) {

    private val Number.dp: Int get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()

    var adapter = LibStringAdapter().apply {
        mode = MODE_NATIVE
    }
    var tvTitle: TextView
    private var mode = MODE_SOR_BY_SIZE

    init {
        View.inflate(context, R.layout.layout_lib_dialog_title, this)

        tvTitle = findViewById(R.id.tv_title)

        val ibSort = findViewById<ImageButton>(R.id.ib_sort)
        ibSort.setOnClickListener {
            mode = if (mode == MODE_SOR_BY_SIZE) {
                adapter.setList(adapter.data.sortedByDescending {
                    NativeLibMap.MAP.containsKey(
                        it.name
                    )
                })
                MODE_SOR_BY_LIB
            } else {
                adapter.setList(adapter.data.sortedByDescending { it.size })
                MODE_SOR_BY_SIZE
            }
        }

        val rvList = findViewById<RecyclerView>(R.id.rv_list)
        rvList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NativeLibView.adapter
        }
    }

}