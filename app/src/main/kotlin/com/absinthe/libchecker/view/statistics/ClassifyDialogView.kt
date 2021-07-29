package com.absinthe.libchecker.view.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.app.IHeaderView
import me.zhanghai.android.fastscroll.FastScrollerBuilder

@SuppressLint("ViewConstructor")
class ClassifyDialogView(context: Context, val lifecycleScope: LifecycleCoroutineScope) :
  AViewGroup(context), IHeaderView {

  val adapter by unsafeLazy { AppAdapter(lifecycleScope) }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.app_bundle)
  }

  private val list = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    layoutManager = LinearLayoutManager(context)
    adapter = this@ClassifyDialogView.adapter
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    setHasFixedSize(true)
    FastScrollerBuilder(this).useMd2Style().build()
  }

  init {
    addPaddingTop(16.dp)
    adapter.setOnItemClickListener { _, _, position ->
      LCAppUtils.launchDetailPage(context as BaseActivity<*>, adapter.getItem(position))
    }
    addView(header)
    addView(list)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    list.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      list.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        header.measuredHeight +
        list.marginTop +
        list.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
    header.layout(0, paddingTop)
    list.layout(paddingStart, header.bottom + list.marginTop)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    adapter.release()
  }
}
