package com.absinthe.libchecker.domain.statistics.chart.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.domain.app.list.ui.adapter.AppAdapter
import com.absinthe.libchecker.domain.statistics.chart.model.AndroidVersionLabelDisplayData
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogAction
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.domain.statistics.chart.ui.view.AndroidVersionLabelView
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.EmptyListView
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class ClassifyDialogView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (ClassifyDialogAction) -> Unit = {}
  private val adapter = AppAdapter()

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT,
      1.0f
    ).also {
      it.topMargin = 4.dp
    }
    layoutManager = LinearLayoutManager(context)
    adapter = this@ClassifyDialogView.adapter
    overScrollMode = OVER_SCROLL_NEVER
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
        data.getOrNull(position)?.let {
          onAction(ClassifyDialogAction.OpenApp(it))
        }
      }
      stateView =
        EmptyListView(context).apply {
          layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, 500.dp)
        }
      isStateViewEnable = true
    }
    addView(header)
    addView(list)
  }

  private val androidVersionView = AndroidVersionLabelView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setPadding(0, 4.dp, 0, 4.dp)
  }

  fun bind(
    state: ClassifyDialogState,
    onAction: (ClassifyDialogAction) -> Unit
  ) {
    this.onAction = onAction
    header.title.text = state.title
    bindAndroidVersion(state.androidVersion)
    adapter.setItemViewStates(state.itemViewStates)
    adapter.setList(state.items)
  }

  private fun bindAndroidVersion(data: AndroidVersionLabelDisplayData?) {
    if (data == null) {
      if (androidVersionView.parent != null) {
        removeView(androidVersionView)
      }
    } else {
      androidVersionView.bind(data)
      if (androidVersionView.parent == null) {
        addView(androidVersionView, 1)
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
