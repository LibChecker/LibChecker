package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetAction
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetState
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeHeaderState
import com.absinthe.libchecker.domain.snapshot.timenode.ui.adapter.TimeNodeAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.EmptyListView
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import rikka.widget.borderview.BorderRecyclerView

class TimeNodeBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (TimeNodeBottomSheetAction) -> Unit = {}
  private var renderedHeader: TimeNodeHeaderState? = null

  private val adapter = TimeNodeAdapter()

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.dialog_title_change_timestamp)
  }

  private val list = BorderRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      val padding = 16.dp
      it.topMargin = padding
      setPadding(padding, 0, padding, 0)
    }
    adapter = this@TimeNodeBottomSheetView.adapter
    isVerticalScrollBarEnabled = false
    layoutManager = LinearLayoutManager(context)
    overScrollMode = OVER_SCROLL_NEVER
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  private val addApkView = TimeNodeAddApkView(context).apply {
    setOnClickListener {
      val header = renderedHeader as? TimeNodeHeaderState.AddApk ?: return@setOnClickListener
      onAction(TimeNodeBottomSheetAction.AddApk(header.isLeft))
    }
  }

  private val autoRemoveView = TimeNodeAutoRemoveView(context)

  init {
    orientation = VERTICAL
    setPadding(0, 16.dp, 0, 0)
    addView(header)
    addView(list)
    adapter.apply {
      setOnItemClickListener { _, _, position ->
        data.getOrNull(position)?.let {
          onAction(TimeNodeBottomSheetAction.SelectItem(position, it))
        }
      }
      stateView = EmptyListView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          it.bottomMargin = 16.dp
        }
      }
      isStateViewEnable = true
    }
  }

  fun bind(
    state: TimeNodeBottomSheetState,
    onAction: (TimeNodeBottomSheetAction) -> Unit
  ) {
    this.onAction = onAction
    header.title.text = state.title
    bindHeader(state.header)
    adapter.bind(state.listData)
  }

  private fun bindHeader(header: TimeNodeHeaderState) {
    val hasSameType = when (renderedHeader) {
      is TimeNodeHeaderState.AddApk -> header is TimeNodeHeaderState.AddApk
      is TimeNodeHeaderState.AutoRemove -> header is TimeNodeHeaderState.AutoRemove
      null -> false
    }
    renderedHeader = header
    if (!hasSameType) {
      adapter.removeAllHeaderView()
      adapter.addHeaderView(
        when (header) {
          is TimeNodeHeaderState.AddApk -> addApkView
          is TimeNodeHeaderState.AutoRemove -> autoRemoveView
        }
      )
    }
    if (header is TimeNodeHeaderState.AutoRemove) {
      autoRemoveView.bind(header.threshold) {
        onAction(TimeNodeBottomSheetAction.SetAutoRemoveEnabled(it))
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
