package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.domain.app.detail.model.ComponentMenuState
import com.absinthe.libchecker.domain.app.detail.model.LibStringRenderState
import com.absinthe.libchecker.domain.app.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.libchecker.ui.app.MenuOptionItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView

class ComponentMenuView(context: Context) : BottomSheetScaffoldView(context) {
  private val itemAdapter = LibStringAdapter(type = ACTIVITY)
  private val componentStyleDemoView = RecyclerView(context).apply {
    setPadding(0, 8.dp, 0, 8.dp)
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    adapter = itemAdapter
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

  init {
    setPadding(16.dp, 16.dp, 16.dp, 0)
    header.title.text = context.getString(R.string.advanced_menu)
    componentStyleDemoView.isNestedScrollingEnabled = false
    addContentView(
      NestedScrollView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(
          LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, 0, 0, 16.dp)
            addView(itemView)
            addView(componentOptionsLayout)
          }
        )
      }
    )
  }

  fun bind(state: ComponentMenuState, onChanged: (MenuOptionItem, Boolean) -> Unit) {
    bindComponentDemo(state)
    componentOptionsLayout.renderOptions(state.componentOptions, onChanged)
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

  private fun bindComponentDemo(state: ComponentMenuState) {
    itemAdapter.setList(state.componentDemoItems)
    itemAdapter.bind(
      LibStringRenderState(
        itemDisplayOptions = state.itemDisplayOptions,
        colorfulRuleIcon = state.colorfulRuleIcon
      ),
      refreshItems = true
    )
  }
}
