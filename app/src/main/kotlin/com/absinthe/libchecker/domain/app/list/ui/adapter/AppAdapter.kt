package com.absinthe.libchecker.domain.app.list.ui.adapter

import android.content.Context
import android.view.ViewGroup
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemDisplay
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.model.AppListRenderState
import com.absinthe.libchecker.domain.app.list.stableAppListItemIdForKey
import com.absinthe.libchecker.domain.app.list.ui.view.AppItemView
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppAdapter(
  private val cardMode: CardMode = CardMode.NORMAL
) : HighlightAdapter<LCItem>(AppListDiffUtil()) {

  private var renderState = AppListRenderState()
  private val pendingItemViewStateCache = mutableMapOf<String, AppListItemViewState>()
  private var itemViewStyle: AppItemView.Style? = null

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      AppItemView(context, getItemViewStyle(context)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: LCItem) {
    val root = holder.itemView as AppItemView
    root.apply {
      if (cardMode == CardMode.DEMO) {
        setSmoothRoundCorner(20.dp)
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
        setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
      } else {
        radius = 0f
      }
    }
    val viewState = getItemViewState(item)
    root.setItemDisplay(createItemDisplay(item, viewState), highlightText)
  }

  override fun convert(holder: BaseViewHolder, item: LCItem, payloads: List<Any>) {
    if (payloads.any { it === HIGHLIGHT_TEXT_PAYLOAD }) {
      val root = holder.itemView as AppItemView
      val viewState = getItemViewState(item)
      root.setItemIdentityDisplay(createItemDisplay(item, viewState), highlightText)
    } else {
      convert(holder, item)
    }
  }

  override fun getItemId(position: Int): Long {
    if (data.isEmpty() || position >= data.size) {
      return super.getItemId(position)
    }
    return stableAppListItemIdForKey(data[position].packageName)
  }

  fun bind(state: AppListRenderState) {
    renderState = state
    highlightText = state.highlightText
    pendingItemViewStateCache.clear()
  }

  fun notifyHighlightTextChanged() {
    if (data.isNotEmpty()) {
      notifyItemRangeChanged(0, data.size, HIGHLIGHT_TEXT_PAYLOAD)
    }
  }

  private fun getItemViewState(item: LCItem): AppListItemViewState {
    return renderState.itemViewStates[item.packageName]
      ?: pendingItemViewStateCache.getOrPut(item.packageName) {
        AppListItemViewState.createPending(
          context = context,
          item = item,
          options = renderState.fallbackDisplayOptions
        )
      }
  }

  private fun getItemViewStyle(context: Context): AppItemView.Style {
    return itemViewStyle ?: AppItemView.Style.create(context).also {
      itemViewStyle = it
    }
  }

  private fun createItemDisplay(
    item: LCItem,
    viewState: AppListItemViewState
  ): AppListItemDisplay {
    return AppListItemDisplay.create(
      label = item.label,
      packageName = item.packageName,
      viewState = viewState,
      iconPackageInfo = renderState.iconPackageInfos[item.packageName] ?: viewState.packageInfo,
      showMissingPackageStrikeThrough = viewState.isPackageMissing && cardMode != CardMode.DEMO,
      chips = renderState.itemChips[item.packageName].orEmpty()
    )
  }

  enum class CardMode {
    NORMAL,
    DEMO
  }

  private companion object {
    private val HIGHLIGHT_TEXT_PAYLOAD = Any()
  }
}
