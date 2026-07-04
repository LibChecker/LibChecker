package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.EXPORTED
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuLayoutItem
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuLayoutItems
import com.absinthe.libchecker.domain.app.list.ui.adapter.AppAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.rulesbundle.Rule
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView

enum class AdvancedMenuSection {
  /** Option section for app list filtering. */
  Filter,

  /** Option section for app list view. */
  View
}

class AdvancedMenuBSDView(
  context: Context,
  displayOptions: Int,
  itemDisplayOptions: Int,
  colorfulRuleIcon: Boolean,
  onSortOptionsChanged: (Int) -> Unit
) : LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val adapter = AdvancedMenuLayoutAdapter()
  private val layoutItems = buildAdvancedMenuLayoutItems()
  private val demoAdapter = AppAdapter(AppAdapter.CardMode.DEMO, displayOptions)
  private val itemAdapter = LibStringAdapter(
    packageName = Constants.EXAMPLE_PACKAGE,
    type = ACTIVITY,
    itemDisplayOptions = itemDisplayOptions,
    colorfulRuleIcon = colorfulRuleIcon
  )

  private val demoView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = OVER_SCROLL_NEVER
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

  private val sortView = AdvancedMenuSortView(context, displayOptions, onSortOptionsChanged).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 12.dp
    }
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

  private val flexLayout2 = FlexboxLayout(context).apply {
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

  private val itemView = MaterialCardView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setSmoothRoundCorner(20.dp)
    overScrollMode = OVER_SCROLL_NEVER
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))

    val componentStyleDemoView = RecyclerView(context).apply {
      setPadding(0, 8.dp, 0, 8.dp)
      overScrollMode = OVER_SCROLL_NEVER
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
              context.packageName,
              NATIVE,
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
              context.packageName,
              NATIVE,
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
              context.packageName,
              NATIVE,
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
    adapter = this@AdvancedMenuBSDView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = true
    setHasFixedSize(true)

    this@AdvancedMenuBSDView.adapter.apply {
      setList(layoutItems)
    }
  }

  /** Add an option item to the [Filter][AdvancedMenuSection.Filter] section. */
  fun addOptionItemView(
    labelRes: Int,
    isChecked: Boolean,
    onCheckedChanged: (Boolean) -> Unit
  ): AdvancedMenuItemView {
    return addOptionItemView(labelRes, isChecked, AdvancedMenuSection.Filter, onCheckedChanged)
  }

  fun addOptionItemView(
    labelRes: Int,
    isChecked: Boolean,
    section: AdvancedMenuSection,
    onCheckedChanged: (Boolean) -> Unit
  ): AdvancedMenuItemView {
    val flexLayout = when (section) {
      AdvancedMenuSection.Filter -> flexLayout
      AdvancedMenuSection.View -> flexLayout2
    }
    val view = AdvancedMenuItemView(context).apply {
      setOption(labelRes, isChecked, onCheckedChanged)
    }
    flexLayout.addView(view)
    return view
  }

  fun addOptionItemViewForItem(
    labelRes: Int,
    isChecked: Boolean,
    onCheckedChanged: (Boolean) -> Unit
  ): AdvancedMenuItemView {
    val view = AdvancedMenuItemView(context).apply {
      setOption(labelRes, isChecked, onCheckedChanged)
    }
    itemFlexLayout.addView(view)
    return view
  }

  fun updateDemoView(displayOptions: Int) {
    demoAdapter.setFallbackDisplayOptions(displayOptions)
    demoAdapter.notifyItemChanged(0)
  }

  fun updateItemDemoView(itemDisplayOptions: Int) {
    itemAdapter.setItemDisplayOptions(itemDisplayOptions)
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

  private inner class AdvancedMenuLayoutAdapter : BaseQuickAdapter<AdvancedMenuLayoutItem, BaseViewHolder>(0) {

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

    override fun convert(holder: BaseViewHolder, item: AdvancedMenuLayoutItem) {
      val container = holder.itemView as LinearLayout
      val child = when (item) {
        AdvancedMenuLayoutItem.AppDemo -> demoView
        AdvancedMenuLayoutItem.Sort -> sortView
        AdvancedMenuLayoutItem.FilterOptions -> flexLayout
        AdvancedMenuLayoutItem.ViewOptions -> flexLayout2
        AdvancedMenuLayoutItem.ComponentStyleDemo -> itemView
        AdvancedMenuLayoutItem.ComponentStyleOptions -> itemFlexLayout
      }
      container.setSingleChild(child)
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
