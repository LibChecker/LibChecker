package com.absinthe.libchecker.view.applist

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class AdvancedMenuBSDView(context: Context) : LinearLayout(context), IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val demoAdapter = AppAdapter().also {
    it.cardMode = AppAdapter.CardMode.DEMO
  }

  private val demoView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    adapter = demoAdapter

    demoAdapter.addData(
      LCItem(
        packageName = Constants.EXAMPLE_PACKAGE,
        label = "Example",
        versionName = "2020.3.19",
        versionCode = 1120,
        installedTime = 0,
        lastUpdatedTime = 0,
        isSystem = false,
        abi = Constants.ARMV8.toShort(),
        features = 0,
        targetApi = Build.VERSION.SDK_INT.toShort(),
        variant = 0
      )
    )
  }

  private val sortView = AdvancedMenuSortView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
  }

  private val flexLayout = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    flexWrap = FlexWrap.WRAP
    justifyContent = JustifyContent.FLEX_START
    flexDirection = FlexDirection.ROW
  }

  fun addOptionItemView(labelRes: Int, option: Int): AdvancedMenuItemView {
    val view = AdvancedMenuItemView(context).apply {
      setOption(labelRes, option)
    }
    flexLayout.addView(view)
    return view
  }

  fun updateDemoView() {
    demoAdapter.notifyItemChanged(0)
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(demoView)
    addView(sortView)
    addView(flexLayout)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
