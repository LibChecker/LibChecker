package com.absinthe.libchecker.domain.app.list.ui.adapter

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import coil.dispose
import coil.load
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemIdentityText
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.ui.view.AppItemView
import com.absinthe.libchecker.domain.app.stableAppListItemIdForKey
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppAdapter(
  private val cardMode: CardMode = CardMode.NORMAL,
  private var fallbackDisplayOptions: Int = AdvancedOptions.DEFAULT_OPTIONS
) : HighlightAdapter<LCItem>(AppListDiffUtil()) {

  private val itemViewStateCache = mutableMapOf<String, AppListItemViewState>()
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
    root.container.apply {
      val viewState = getItemViewState(item)
      val packageInfo = viewState.packageInfo
      if (item.packageName != Constants.EXAMPLE_PACKAGE) {
        icon.loadAppIcon(packageInfo, getItemViewStyle(context).newIconPlaceholder(context))
      }
      bindIdentityText(root, item, viewState)

      versionInfo.text = viewState.versionInfo
      setAbiDisplay(viewState)
      setPackageBadge(viewState.packageBadge)
    }
  }

  override fun convert(holder: BaseViewHolder, item: LCItem, payloads: List<Any>) {
    if (payloads.any { it === HIGHLIGHT_TEXT_PAYLOAD }) {
      val root = holder.itemView as AppItemView
      bindIdentityText(root, item, getItemViewState(item))
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

  fun setItemViewStates(itemViewStates: Map<String, AppListItemViewState>) {
    itemViewStateCache.clear()
    itemViewStateCache.putAll(itemViewStates)
  }

  fun putItemViewStates(itemViewStates: Map<String, AppListItemViewState>) {
    itemViewStateCache.putAll(itemViewStates)
  }

  fun clearItemViewStateCache() {
    itemViewStateCache.clear()
  }

  fun notifyHighlightTextChanged() {
    if (data.isNotEmpty()) {
      notifyItemRangeChanged(0, data.size, HIGHLIGHT_TEXT_PAYLOAD)
    }
  }

  fun setFallbackDisplayOptions(options: Int) {
    fallbackDisplayOptions = options
    clearItemViewStateCache()
  }

  private fun getItemViewState(item: LCItem): AppListItemViewState {
    return itemViewStateCache.getOrPut(item.packageName) {
      AppListItemViewState.createPending(
        context = context,
        item = item,
        options = fallbackDisplayOptions
      )
    }
  }

  private fun getItemViewStyle(context: Context): AppItemView.Style {
    return itemViewStyle ?: AppItemView.Style.create(context).also {
      itemViewStyle = it
    }
  }

  private fun bindIdentityText(
    root: AppItemView,
    item: LCItem,
    viewState: AppListItemViewState
  ) {
    val identityText = AppListItemIdentityText.create(
      label = item.label,
      packageName = item.packageName,
      versionInfo = viewState.versionInfo,
      accessibilityAbiInfo = viewState.accessibilityAbiInfo,
      showMissingPackageStrikeThrough = viewState.isPackageMissing && cardMode != CardMode.DEMO
    )
    root.container.setIdentityText(identityText, highlightText)
    root.setItemContentDescription(identityText.contentDescription)
  }

  enum class CardMode {
    NORMAL,
    DEMO
  }

  private companion object {
    private val HIGHLIGHT_TEXT_PAYLOAD = Any()
  }
}

private fun AppCompatImageView.loadAppIcon(packageInfo: PackageInfo?, placeholderDrawable: Drawable?) {
  if (packageInfo == null) {
    dispose()
    setImageDrawable(placeholderDrawable)
    return
  }
  load(packageInfo) {
    placeholder(placeholderDrawable)
    error(placeholderDrawable)
  }
}
