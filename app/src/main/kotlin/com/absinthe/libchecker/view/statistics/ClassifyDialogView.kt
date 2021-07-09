package com.absinthe.libchecker.view.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.extensions.*
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import me.zhanghai.android.fastscroll.FastScrollerBuilder

@SuppressLint("ViewConstructor")
class ClassifyDialogView(context: Context, val lifecycleScope: LifecycleCoroutineScope) : LinearLayout(context) {

    val adapter by lazy { AppAdapter(lifecycleScope) }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addPaddingTop(16.dp)
        adapter.setOnItemClickListener { _, _, position ->
            val intent = Intent(context, AppDetailActivity::class.java).apply {
                putExtras(Bundle().apply {
                    val item = adapter.getItem(position)
                    putString(EXTRA_PACKAGE_NAME, item.packageName)
                    putParcelable(EXTRA_DETAIL_BEAN, DetailExtraBean(item.isSplitApk, item.isKotlinUsed, item.variant))
                })
            }
            context.startActivity(intent)
        }

        val rvList = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
            adapter = this@ClassifyDialogView.adapter
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            clipChildren = false
            setHasFixedSize(true)
            FastScrollerBuilder(this).useMd2Style().build()
        }

        addView(rvList)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter.release()
    }

}
