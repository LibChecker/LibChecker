package com.absinthe.libchecker.domain.snapshot.list.ui.view

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.adapter.AdvancedMenuAdapter
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
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

  private val adapter = AdvancedMenuAdapter()

  private val demoView = SnapshotItemView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    render(demoDisplayData)
  }

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
      addData(demoView)
      addData(flexLayout)
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
    demoView.render(data)
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
