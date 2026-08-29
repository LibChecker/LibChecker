package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetAction
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetState
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeHeaderState
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.adapter.addSpacingDecoration
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libchecker.view.app.EmptyListView
import rikka.widget.borderview.BorderRecyclerView

class TimeNodeBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private var onAction: (TimeNodeBottomSheetAction) -> Unit = {}
  private var renderedHeader: TimeNodeHeaderState? = null
  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()

  private val adapter = BindOnlyAdapter<SnapshotTimeNodeItem, TimeNodeItemView>(
    ::TimeNodeItemView
  ) { item ->
    bind(item, packageIconSources)
  }

  private val list = BorderRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      val padding = 12.dp
      setPadding(padding, 0, padding, 0)
    }
    adapter = this@TimeNodeBottomSheetView.adapter
    isVerticalScrollBarEnabled = false
    layoutManager = LinearLayoutManager(context)
    overScrollMode = OVER_SCROLL_NEVER
    addSpacingDecoration(3.dp)
  }

  private val addApkView = TimeNodeAddApkView(context).apply {
    setOnClickListener {
      val header = renderedHeader as? TimeNodeHeaderState.AddApk ?: return@setOnClickListener
      onAction(TimeNodeBottomSheetAction.AddApk(header.isLeft))
    }
  }

  private val autoRemoveView = TimeNodeAutoRemoveView(context)

  init {
    setPadding(0, 8.dp, 0, 0)
    header.title.text = context.getString(R.string.dialog_title_change_timestamp)
    addContentView(list)
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
      isUseStateViewSize = true
    }
  }

  fun bind(
    state: TimeNodeBottomSheetState,
    onAction: (TimeNodeBottomSheetAction) -> Unit
  ) {
    this.onAction = onAction
    header.title.text = state.title
    bindHeader(state.header)
    packageIconSources = state.listData.packageIconSources
    adapter.setList(state.listData.items)
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
}
