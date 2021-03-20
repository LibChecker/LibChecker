package com.absinthe.libchecker.view.statistics

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.extensions.*
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class ClassifyDialogView(context: Context) : LinearLayout(context) {

    var adapter = AppAdapter()

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        paddingTopCompat = 16.dp
        clipToPadding = false

        adapter.setOnItemClickListener { _, view, position ->
            val intent = Intent(context, AppDetailActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, adapter.getItem(position).packageName)
                })
            }

            val options = ActivityOptions.makeSceneTransitionAnimation(
                (context as AppCompatActivity), view, view.transitionName
            )

            if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
        }

        val rvList = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
            adapter = this@ClassifyDialogView.adapter
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            setHasFixedSize(true)
            addSystemBarPadding(addStatusBarPadding = false)
            FastScrollerBuilder(this).useMd2Style().build()
        }

        addView(rvList)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter.release()
    }

}