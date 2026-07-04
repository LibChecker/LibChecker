package com.absinthe.libchecker.domain.snapshot.list.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotMenuLayoutItem
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotMenuLayoutItems
import com.absinthe.libchecker.domain.snapshot.list.model.replaceSnapshotMenuDemoDisplayData
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class SnapshotMenuBSDView(
  context: Context,
  demoDisplayData: SnapshotItemDisplayData
) : LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private var layoutItems = buildSnapshotMenuLayoutItems(demoDisplayData)
  private val adapter = SnapshotMenuAdapter()

  private val flexLayout = FlexboxLayout(context).apply {
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

    this@SnapshotMenuBSDView.adapter.apply {
      setList(layoutItems)
    }
  }

  fun addOptionItemView(
    labelRes: Int,
    option: Int,
    currentOptions: Int
  ): SnapshotMenuItemView {
    val view = SnapshotMenuItemView(context).apply {
      setOption(labelRes, option, currentOptions)
    }
    flexLayout.addView(view)
    return view
  }

  fun setDemoDisplayData(data: SnapshotItemDisplayData) {
    layoutItems = layoutItems.replaceSnapshotMenuDemoDisplayData(data)
    adapter.setList(layoutItems)
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
          container.setSingleChild(flexLayout)
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
