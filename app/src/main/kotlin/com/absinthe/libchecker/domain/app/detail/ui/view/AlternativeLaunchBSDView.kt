package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchAction
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchItem
import com.absinthe.libchecker.domain.app.detail.ui.adapter.AlternativeLaunchAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AlternativeLaunchBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val adapter = AlternativeLaunchAdapter()

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.alternative_launch_method)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = OVER_SCROLL_NEVER
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

  override fun getHeaderView(): BottomSheetHeaderView = header
}
