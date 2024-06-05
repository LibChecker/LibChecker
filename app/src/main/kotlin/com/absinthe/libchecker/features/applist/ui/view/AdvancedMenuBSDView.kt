package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.features.applist.ui.adapter.AppAdapter
import com.absinthe.libchecker.features.home.ui.adapter.AdvancedMenuAdapter
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.rulesbundle.Rule
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView

class AdvancedMenuBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val adapter = AdvancedMenuAdapter()
  private val demoAdapter = AppAdapter(AppAdapter.CardMode.DEMO)
  private val itemAdapter = LibStringAdapter(Constants.EXAMPLE_PACKAGE, ACTIVITY)

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

  private val itemView = MaterialCardView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))

    val componentStyleDemoView = RecyclerView(context).apply {
      setPadding(0, 8.dp, 0, 8.dp)
      overScrollMode = RecyclerView.OVER_SCROLL_NEVER
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
      adapter = itemAdapter

      itemAdapter.apply {
        addData(
          LibStringItemChip(
            LibStringItem(
              name = Constants.EXAMPLE_EXPORTED,
              source = EXPORTED
            ),
            Rule(
              Constants.EXAMPLE_RULE,
              R.drawable.ic_logo,
              null,
              null,
              true
            )
          )
        )
        addData(
          LibStringItemChip(
            LibStringItem(
              name = Constants.EXAMPLE_NORMAL
            ),
            Rule(
              Constants.EXAMPLE_RULE,
              R.drawable.ic_logo,
              null,
              null,
              true
            )
          )
        )
        addData(
          LibStringItemChip(
            LibStringItem(
              name = Constants.EXAMPLE_DISABLED,
              source = DISABLED
            ),
            Rule(
              Constants.EXAMPLE_RULE,
              R.drawable.ic_logo,
              null,
              null,
              true
            )
          )
        )
      }
    }

    addView(componentStyleDemoView)
  }

  private val itemFlexLayout = FlexboxLayout(context).apply {
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

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = this@AdvancedMenuBSDView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = true
    setHasFixedSize(true)

    this@AdvancedMenuBSDView.adapter.apply {
      addData(demoView)
      addData(sortView)
      addData(flexLayout)
      addData(itemView)
      addData(itemFlexLayout)
    }
  }

  fun addOptionItemView(labelRes: Int, option: Int): AdvancedMenuItemView {
    val view = AdvancedMenuItemView(context).apply {
      setOption(labelRes, option)
    }
    flexLayout.addView(view)
    return view
  }

  fun addOptionItemViewForItem(labelRes: Int, option: Int): AdvancedMenuItemView {
    val view = AdvancedMenuItemView(context).apply {
      setItemOption(labelRes, option)
    }
    itemFlexLayout.addView(view)
    return view
  }

  fun updateDemoView() {
    demoAdapter.notifyItemChanged(0)
  }

  fun updateItemDemoView() {
    itemAdapter.notifyItemChanged(0)
    itemAdapter.notifyItemChanged(1)
    itemAdapter.notifyItemChanged(2)
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
