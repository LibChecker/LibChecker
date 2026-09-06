package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuAction
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuBottomSheetState
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuLayoutItem
import com.absinthe.libchecker.domain.app.list.model.AppListRenderState
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuLayoutItems
import com.absinthe.libchecker.domain.app.list.ui.adapter.AppAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.libchecker.ui.app.MenuOptionItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setSingleChild
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class AdvancedMenuBSDView(context: Context) : BottomSheetScaffoldView(context) {

  private var onAction: (AdvancedMenuAction) -> Unit = {}

  private val adapter = AdvancedMenuLayoutAdapter()
  private val layoutItems = buildAdvancedMenuLayoutItems()
  private val demoAdapter = AppAdapter(AppAdapter.CardMode.DEMO)

  private val demoView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    overScrollMode = OVER_SCROLL_NEVER
    isNestedScrollingEnabled = false
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
  private val abiOptionsLayout = createOptionsLayout().apply {
    (layoutParams as LayoutParams).topMargin = 0
  }
  private val filterSection = LinearLayout(context).apply {
    orientation = VERTICAL
    addView(sectionTitle(R.string.adv_app_filter))
    addView(filterOptionsLayout)
    addView(abiOptionsLayout)
  }
  private val appearanceSection = LinearLayout(context).apply {
    orientation = VERTICAL
    addView(sectionTitle(R.string.adv_app_appearance, topMarginDp = 0))
    addView(demoView)
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
    setPadding(0, 0, 0, 16.dp)
    isNestedScrollingEnabled = true
    setHasFixedSize(true)

    this@AdvancedMenuBSDView.adapter.setList(layoutItems)
  }

  init {
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    header.title.text = context.getString(R.string.advanced_menu)
    addContentView(list)
  }

  fun bind(
    state: AdvancedMenuBottomSheetState,
    onAction: (AdvancedMenuAction) -> Unit
  ) {
    this.onAction = onAction
    demoAdapter.bind(
      AppListRenderState(fallbackDisplayOptions = state.displayOptions),
      refreshItems = true
    )
    demoAdapter.setList(listOf(state.demoItem))
    sortView.bind(state.displayOptions) {
      this.onAction(AdvancedMenuAction.SortChanged(it))
    }
    filterOptionsLayout.renderOptions(state.filterOptions.take(3)) { item, isChecked ->
      this.onAction(
        AdvancedMenuAction.DisplayOptionChanged(
          item = item,
          isChecked = isChecked
        )
      )
    }
    abiOptionsLayout.renderOptions(state.filterOptions.drop(3)) { item, isChecked ->
      this.onAction(AdvancedMenuAction.DisplayOptionChanged(item, isChecked))
    }
    viewOptionsLayout.renderOptions(state.viewOptions) { item, isChecked ->
      this.onAction(
        AdvancedMenuAction.DisplayOptionChanged(
          item = item,
          isChecked = isChecked
        )
      )
    }
  }

  override fun onDetachedFromWindow() {
    onAction = {}
    super.onDetachedFromWindow()
  }

  private fun sectionTitle(titleRes: Int, topMarginDp: Int = 12) = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
      it.topMargin = topMarginDp.dp
    }
    setTypeface(null, Typeface.BOLD)
    setText(titleRes)
    ViewCompat.setAccessibilityHeading(this, true)
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
    items.forEachIndexed { index, item ->
      val itemView = getChildAt(index) as? MenuOptionItemView
        ?: MenuOptionItemView(
          context = context,
          dispatchCheckedChangeImmediately = true
        ).also { addView(it, index) }
      itemView.bind(item) { isChecked -> onCheckedChanged(item, isChecked) }
    }
    while (childCount > items.size) {
      removeViewAt(childCount - 1)
    }
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
        AdvancedMenuLayoutItem.AppDemo -> appearanceSection
        AdvancedMenuLayoutItem.Sort -> sortView
        AdvancedMenuLayoutItem.FilterOptions -> filterSection
        AdvancedMenuLayoutItem.ViewOptions -> viewOptionsLayout
      }
      container.setSingleChild(child)
    }
  }
}
