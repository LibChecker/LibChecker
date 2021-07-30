package com.absinthe.libchecker.view.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.app.IHeaderView
import me.zhanghai.android.fastscroll.FastScrollerBuilder

@SuppressLint("ViewConstructor")
class ClassifyDialogView(context: Context, val lifecycleScope: LifecycleCoroutineScope) :
  LinearLayout(context), IHeaderView {

  val adapter by unsafeLazy { AppAdapter(lifecycleScope) }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  private val list = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
      1.0f
    ).also {
      it.topMargin = 24.dp
    }
    layoutManager = LinearLayoutManager(context)
    adapter = this@ClassifyDialogView.adapter
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    setHasFixedSize(true)
    FastScrollerBuilder(this).useMd2Style().build()
  }

  init {
    orientation = VERTICAL
    addPaddingTop(16.dp)
    adapter.apply {
      setOnItemClickListener { _, _, position ->
        LCAppUtils.launchDetailPage(context as BaseActivity<*>, adapter.getItem(position))
      }
      setEmptyView(
        LibReferenceLoadingView(context).apply {
          layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500.dp)
        }
      )
    }
    addView(header)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    adapter.release()
  }
}
