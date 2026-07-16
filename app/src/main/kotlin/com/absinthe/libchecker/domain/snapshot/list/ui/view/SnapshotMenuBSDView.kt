package com.absinthe.libchecker.domain.snapshot.list.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotMenuAction
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotMenuBottomSheetState
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotMenuLayoutItem
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotMenuLayoutItems
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.libchecker.ui.app.MenuOptionItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class SnapshotMenuBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (SnapshotMenuAction) -> Unit = {}

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val adapter = SnapshotMenuAdapter()

  private val optionsLayout = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    flexWrap = FlexWrap.WRAP
    justifyContent = JustifyContent.FLEX_START
    flexDirection = FlexDirection.ROW
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    overScrollMode = OVER_SCROLL_NEVER
    adapter = this@SnapshotMenuBSDView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = true
    setHasFixedSize(true)
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(list)
  }

  fun bind(
    state: SnapshotMenuBottomSheetState,
    onAction: (SnapshotMenuAction) -> Unit
  ) {
    this.onAction = onAction
    optionsLayout.renderOptions(state.options)
    adapter.setList(buildSnapshotMenuLayoutItems(state.demoDisplayData))
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  override fun onDetachedFromWindow() {
    onAction = {}
    super.onDetachedFromWindow()
  }

  private fun FlexboxLayout.renderOptions(items: List<MenuOptionItem>) {
    removeAllViews()
    items.forEach { item ->
      addView(
        MenuOptionItemView(context).apply {
          bind(item) { isChecked ->
            onAction(
              SnapshotMenuAction.OptionChanged(
                item = item,
                isChecked = isChecked
              )
            )
          }
        }
      )
    }
  }

  private inner class SnapshotMenuAdapter : BaseQuickAdapter<SnapshotMenuLayoutItem, BaseViewHolder>(0) {

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
      return BaseViewHolder(
        LinearLayout(context).apply {
          layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          orientation = VERTICAL
        }
      )
    }

    override fun convert(holder: BaseViewHolder, item: SnapshotMenuLayoutItem) {
      val container = holder.itemView as LinearLayout
      when (item) {
        is SnapshotMenuLayoutItem.Demo -> {
          val demoView = container.ensureSnapshotDemoView()
          demoView.render(item.displayData)
        }

        SnapshotMenuLayoutItem.Options -> {
          container.setSingleChild(optionsLayout)
        }
      }
    }

    private fun LinearLayout.ensureSnapshotDemoView(): SnapshotItemView {
      val child = getChildAt(0)
      if (childCount == 1 && child is SnapshotItemView) {
        return child
      }
      removeAllViews()
      return SnapshotItemView(context).apply {
        layoutParams = LayoutParams(
          LayoutParams.MATCH_PARENT,
          LayoutParams.WRAP_CONTENT
        ).also {
          it.topMargin = 24.dp
        }
      }.also(::addView)
    }

    private fun LinearLayout.setSingleChild(child: View) {
      if (childCount == 1 && getChildAt(0) === child) {
        return
      }
      (child.parent as? ViewGroup)?.removeView(child)
      removeAllViews()
      addView(child)
    }
  }
}
