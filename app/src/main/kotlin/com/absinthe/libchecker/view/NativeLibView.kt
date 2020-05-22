package com.absinthe.libchecker.view

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.recyclerview.MODE_NATIVE
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.SPUtils
import com.squareup.contour.ContourLayout

class NativeLibView(context: Context) : ContourLayout(context) {

    val adapter = LibStringAdapter().apply {
        mode = MODE_NATIVE
    }
    val tvTitle: TextView = TextView(context).apply {
        setTextColor(ContextCompat.getColor(context, R.color.textNormal))
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        setTypeface(null, Typeface.BOLD)
        applyLayout(
            x = leftTo { parent.left() + 25.dip },
            y = topTo { parent.top() + 20.dip }
        )
    }

    val ibSort: ImageButton = ImageButton(context).apply {
        visibility = View.GONE

        val outValue = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.selectableItemBackgroundBorderless,
            outValue,
            true
        )
        setBackgroundResource(outValue.resourceId)

        setImageResource(R.drawable.ic_lib_sort)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tooltipText = context.getString(R.string.menu_sort)
        }

        setOnClickListener {
            GlobalValues.libSortMode.value =
                if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
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
            SPUtils.putInt(
                context,
                Constants.PREF_LIB_SORT_MODE,
                GlobalValues.libSortMode.value ?: MODE_SORT_BY_SIZE
            )
        }

        applyLayout(
            x = rightTo { parent.right() - 25.dip },
            y = centerVerticallyTo { tvTitle.centerY() }
        )
    }

    private val rvList: RecyclerView = RecyclerView(context).apply {
        clipToPadding = false
        overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        layoutManager = LinearLayoutManager(context)
        adapter = this@NativeLibView.adapter
        setPadding(25.dip, 0, 25.dip, 0)
        applyLayout(
            x = matchParentX(),
            y = topTo { tvTitle.bottom() + 10.dip }
        )
    }

    init {
        contourHeightOf { rvList.bottom() + 10.dip }
    }

}