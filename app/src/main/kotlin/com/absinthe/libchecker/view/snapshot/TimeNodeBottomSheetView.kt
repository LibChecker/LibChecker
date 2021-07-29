package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.TimeNodeAdapter
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.app.IHeaderView

class TimeNodeBottomSheetView(context: Context) : AViewGroup(context), IHeaderView {

  val adapter by unsafeLazy { TimeNodeAdapter() }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.dialog_title_change_timestamp)
  }

  private val list = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = this@TimeNodeBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  init {
    setPadding(24.dp, 16.dp, 24.dp, 0)
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
      paddingTop
        + header.measuredHeight
        + list.marginTop
        + list.measuredHeight
        + paddingBottom
    )
  }

  override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
    header.layout(0, paddingTop)
    list.layout(paddingStart, header.bottom + list.marginTop)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
