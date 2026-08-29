package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchAction
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchItem
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.adapter.addSpacingDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView

class AlternativeLaunchBSDView(context: Context) : BottomSheetScaffoldView(context) {

  private val adapter = BindOnlyAdapter(::AlternativeLaunchItemView, AlternativeLaunchItemView::bind)

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    overScrollMode = OVER_SCROLL_NEVER
    adapter = this@AlternativeLaunchBSDView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    setHasFixedSize(true)
    addSpacingDecoration(4.dp)
  }

  init {
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    header.title.text = context.getString(R.string.alternative_launch_method)
    addView(list)
  }

  fun bind(
    items: List<AlternativeLaunchItem>,
    onAction: (AlternativeLaunchAction) -> Unit
  ) {
    adapter.setOnItemClickListener { _, _, position ->
      adapter.data.getOrNull(position)?.let {
        onAction(AlternativeLaunchAction.OpenActivity(it))
      }
    }
    adapter.setList(items)
  }
}
