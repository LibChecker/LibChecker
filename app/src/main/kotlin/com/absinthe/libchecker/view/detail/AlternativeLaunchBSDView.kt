package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.BottomSheetRecyclerView
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.detail.AlternativeLaunchAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AlternativeLaunchBSDView(context: Context) : LinearLayout(context), IHeaderView {

  val adapter by unsafeLazy { AlternativeLaunchAdapter() }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.alternative_launch_method)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = this@AlternativeLaunchBSDView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    setHasFixedSize(true)
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
