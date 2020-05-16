package com.absinthe.libchecker.view

import android.content.Context
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
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.SPUtils

class NativeLibView(context: Context) : ConstraintLayout(context) {

    var adapter = LibStringAdapter().apply {
        mode = MODE_NATIVE
    }
    var tvTitle: TextView

    init {
        View.inflate(context, R.layout.layout_dialog_lib_list, this)

        tvTitle = findViewById(R.id.tv_title)

        val ibSort = findViewById<ImageButton>(R.id.ib_sort)
        ibSort.setOnClickListener {
            GlobalValues.libSortMode.value = if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                adapter.setList(adapter.data.sortedByDescending {
                    NativeLibMap.MAP.containsKey(
                        it.name
                    )
                })
                MODE_SORT_BY_LIB
            } else {
                adapter.setList(adapter.data.sortedByDescending { it.size })
                MODE_SORT_BY_SIZE
            }
            SPUtils.putInt(context, Constants.PREF_LIB_SORT_MODE, GlobalValues.libSortMode.value?: MODE_SORT_BY_SIZE)
        }

        val rvList = findViewById<RecyclerView>(R.id.rv_list)
        rvList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NativeLibView.adapter
        }
    }

}