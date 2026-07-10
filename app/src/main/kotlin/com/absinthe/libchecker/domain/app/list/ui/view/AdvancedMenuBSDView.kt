package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.domain.app.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuAction
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuBottomSheetState
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuLayoutItem
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuLayoutItems
import com.absinthe.libchecker.domain.app.list.ui.adapter.AppAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.libchecker.ui.app.MenuOptionItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView

class AdvancedMenuBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (AdvancedMenuAction) -> Unit = {}

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val adapter = AdvancedMenuLayoutAdapter()
  private val layoutItems = buildAdvancedMenuLayoutItems()
  private val demoAdapter = AppAdapter(AppAdapter.CardMode.DEMO)
  private var itemAdapter: LibStringAdapter? = null
  private var itemAdapterColorfulRuleIcon: Boolean? = null

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
  }

  private val sortView = AdvancedMenuSortView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 12.dp
    }
  }

  private val filterOptionsLayout = createOptionsLayout()
  private val viewOptionsLayout = createOptionsLayout()

  private val componentStyleDemoView = RecyclerView(context).apply {
    setPadding(0, 8.dp, 0, 8.dp)
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
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
    setCardBackgroundColor(
      context.getColorStateListByAttr(
        com.google.android.material.R.attr.colorSurfaceContainerHigh
      )
    )
    addView(componentStyleDemoView)
  }

  private val componentOptionsLayout = createOptionsLayout()

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

    this@AdvancedMenuBSDView.adapter.setList(layoutItems)
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(list)
  }

  fun bind(
    state: AdvancedMenuBottomSheetState,
    onAction: (AdvancedMenuAction) -> Unit
  ) {
    this.onAction = onAction
    demoAdapter.setFallbackDisplayOptions(state.displayOptions)
    demoAdapter.setList(listOf(state.demoItem))
    sortView.bind(state.displayOptions) {
      this.onAction(AdvancedMenuAction.SortChanged(it))
    }
    filterOptionsLayout.renderOptions(state.filterOptions) { item, isChecked ->
      this.onAction(
        AdvancedMenuAction.DisplayOptionChanged(
          item = item,
          isChecked = isChecked
        )
      )
    }
    viewOptionsLayout.renderOptions(state.viewOptions) { item, isChecked ->
      this.onAction(
        AdvancedMenuAction.DisplayOptionChanged(
          item = item,
          isChecked = isChecked
        )
      )
    }
    componentOptionsLayout.renderOptions(state.componentOptions) { item, isChecked ->
      this.onAction(
        AdvancedMenuAction.ItemDisplayOptionChanged(
          item = item,
          isChecked = isChecked
        )
      )
    }
    bindComponentDemo(state)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  override fun onDetachedFromWindow() {
    onAction = {}
    super.onDetachedFromWindow()
  }

  private fun createOptionsLayout(): FlexboxLayout {
    return FlexboxLayout(context).apply {
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
  }

  private fun FlexboxLayout.renderOptions(
    items: List<MenuOptionItem>,
    onCheckedChanged: (MenuOptionItem, Boolean) -> Unit
  ) {
    removeAllViews()
    items.forEach { item ->
      addView(
        MenuOptionItemView(context).apply {
          bind(item) { isChecked -> onCheckedChanged(item, isChecked) }
        }
      )
    }
  }

  private fun bindComponentDemo(state: AdvancedMenuBottomSheetState) {
    val currentAdapter = itemAdapter
    val adapter = if (
      currentAdapter == null ||
      itemAdapterColorfulRuleIcon != state.colorfulRuleIcon
    ) {
      LibStringAdapter(
        type = ACTIVITY,
        itemDisplayOptions = state.itemDisplayOptions,
        colorfulRuleIcon = state.colorfulRuleIcon
      ).also {
        itemAdapter = it
        itemAdapterColorfulRuleIcon = state.colorfulRuleIcon
        componentStyleDemoView.adapter = it
      }
    } else {
      currentAdapter
    }
    adapter.setItemDisplayOptions(state.itemDisplayOptions)
    adapter.setList(state.componentDemoItems)
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
        AdvancedMenuLayoutItem.FilterOptions -> filterOptionsLayout
        AdvancedMenuLayoutItem.ViewOptions -> viewOptionsLayout
        AdvancedMenuLayoutItem.ComponentStyleDemo -> itemView
        AdvancedMenuLayoutItem.ComponentStyleOptions -> componentOptionsLayout
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
