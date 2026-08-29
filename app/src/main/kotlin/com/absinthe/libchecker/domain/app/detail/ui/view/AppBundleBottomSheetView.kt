package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.adapter.addSpacingDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libraries.utils.manager.SystemBarManager

class AppBundleBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private val adapter = BindOnlyAdapter(::AppBundleItemView, AppBundleItemView::bind)

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    overScrollMode = OVER_SCROLL_NEVER
    adapter = this@AppBundleBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    setHasFixedSize(true)
    addSpacingDecoration(4.dp)
  }

  init {
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    header.title.text = context.getString(R.string.app_bundle)
    addView(list)
  }

  fun bind(items: List<AppBundleItem>) {
    adapter.setList(items)
  }
}
