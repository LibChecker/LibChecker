package com.absinthe.libchecker.view.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import me.zhanghai.android.fastscroll.FastScrollerBuilder

@SuppressLint("ViewConstructor")
class ClassifyDialogView(context: Context, val lifecycleScope: LifecycleCoroutineScope) : LinearLayout(context) {

    val adapter by unsafeLazy { AppAdapter(lifecycleScope) }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addPaddingTop(16.dp)
        adapter.setOnItemClickListener { _, _, position ->
            val item = adapter.getItem(position)
            val intent = Intent(context, AppDetailActivity::class.java).apply {
                putExtras(
                    bundleOf(
                        EXTRA_PACKAGE_NAME to item.packageName,
                        EXTRA_DETAIL_BEAN to DetailExtraBean(
                            item.isSplitApk,
                            item.isKotlinUsed,
                            item.variant
                        )
                    )
                )
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
