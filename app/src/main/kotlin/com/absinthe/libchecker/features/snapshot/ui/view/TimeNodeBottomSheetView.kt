package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.snapshot.ui.adapter.TimeNodeAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import rikka.widget.borderview.BorderRecyclerView

class TimeNodeBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  val adapter by unsafeLazy { TimeNodeAdapter() }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.dialog_title_change_timestamp)
  }

  private val list = BorderRecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      val padding = 16.dp
      it.topMargin = padding
      setPadding(padding, 0, padding, 0)
    }
    adapter = this@TimeNodeBottomSheetView.adapter
    isVerticalScrollBarEnabled = false
    layoutManager = LinearLayoutManager(context)
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  init {
    orientation = VERTICAL
    setPadding(0, 16.dp, 0, 0)
    addView(header)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
