package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.model.LibStringAction
import com.absinthe.libchecker.domain.app.detail.model.LibStringComponentItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.model.LibStringMetadataItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibStringNativeItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibStringPermissionItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibStringRenderState
import com.absinthe.libchecker.domain.app.detail.model.LibStringStaticItemDisplay
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.domain.app.detail.ui.view.ComponentLibItemView
import com.absinthe.libchecker.domain.app.detail.ui.view.MetadataLibItemView
import com.absinthe.libchecker.domain.app.detail.ui.view.NativeLibItemView
import com.absinthe.libchecker.domain.app.detail.ui.view.RuleChipIconCache
import com.absinthe.libchecker.domain.app.detail.ui.view.StaticLibItemView
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.chad.library.adapter.base.viewholder.BaseViewHolder

private const val HIGHLIGHT_TRANSITION_DURATION = 250

class LibStringAdapter(
  @LibType private val type: Int,
  private val onAction: ((LibStringAction) -> Unit)? = null
) : HighlightAdapter<LibStringItemChip>(LibStringDiffUtil()) {

  private var renderState = LibStringRenderState()
  private val fallbackProcessColors = mutableMapOf<String, Int>()
  private val metadataPreviews = mutableMapOf<MetadataPreviewKey, AppResourcePreview>()

  fun bind(state: LibStringRenderState) {
    renderState = state
    highlightText = state.highlightText
  }

  fun preloadRuleChipIcons(items: List<LibStringItemChip>) {
    if (isItemOptionEnabled(AdvancedOptions.SHOW_MARKED_LIB)) {
      RuleChipIconCache.preload(context, items.asSequence().mapNotNull { it.rule })
    }
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    val itemView = when (type) {
      NATIVE -> NativeLibItemView(context)
      METADATA -> MetadataLibItemView(context)
      STATIC -> StaticLibItemView(context)
      else -> ComponentLibItemView(context)
    }
    itemView.setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
    return createBaseViewHolder(itemView)
  }

  override fun convert(holder: BaseViewHolder, item: LibStringItemChip) {
    when (type) {
      NATIVE -> (holder.itemView as NativeLibItemView).bind(
        display = LibStringNativeItemDisplay.create(item, renderState.itemDisplayOptions),
        highlightText = highlightText,
        colorfulRuleIcon = renderState.colorfulRuleIcon
      )

      PERMISSION -> (holder.itemView as ComponentLibItemView).bind(
        display = LibStringPermissionItemDisplay.create(
          item = item,
          itemDisplayOptions = renderState.itemDisplayOptions,
          notGrantedLabel = context.getString(com.absinthe.libchecker.R.string.permission_not_granted)
        ),
        highlightText = highlightText
      )

      METADATA -> bindMetadata(holder.itemView as MetadataLibItemView, item)

      STATIC -> (holder.itemView as StaticLibItemView).bind(
        display = LibStringStaticItemDisplay.create(item, renderState.itemDisplayOptions),
        highlightText = highlightText,
        colorfulRuleIcon = renderState.colorfulRuleIcon
      )

      else -> bindComponent(holder.itemView as ComponentLibItemView, item)
    }

    bindHighlightBackground(holder)
  }

  fun setMetadataPreview(
    item: LibStringItemChip,
    preview: AppResourcePreview
  ) {
    val key = item.metadataPreviewKey()
    if (preview == AppResourcePreview.Original) {
      metadataPreviews.remove(key)
    } else {
      metadataPreviews[key] = preview
    }
    data.indexOfFirst { it.metadataPreviewKey() == key }
      .takeIf { it >= 0 }
      ?.let(::notifyItemChanged)
  }

  private fun bindComponent(
    itemView: ComponentLibItemView,
    item: LibStringItemChip
  ) {
    val processName = item.item.process.takeIf { renderState.processMode && !it.isNullOrEmpty() }
    val processIndicatorColor = processName?.let {
      renderState.processColors[it] ?: fallbackProcessColors.getOrPut(it, UiUtils::getRandomColor)
    }
    itemView.bind(
      display = LibStringComponentItemDisplay.create(
        item = item,
        type = type,
        itemDisplayOptions = renderState.itemDisplayOptions,
        processMode = renderState.processMode,
        processIndicatorColor = processIndicatorColor
      ),
      highlightText = highlightText,
      colorfulRuleIcon = renderState.colorfulRuleIcon
    )
  }

  private fun bindMetadata(
    itemView: MetadataLibItemView,
    item: LibStringItemChip
  ) {
    val display = LibStringMetadataItemDisplay.create(
      item = item,
      itemDisplayOptions = renderState.itemDisplayOptions,
      apkPreviewUnavailableLabel = context.getString(
        com.absinthe.libchecker.R.string.apk_preview_item_not_available
      ),
      preview = metadataPreviews[item.metadataPreviewKey()] ?: AppResourcePreview.Original
    )
    itemView.bind(
      display = display,
      highlightText = highlightText,
      onResourceClick = onAction?.let { callback ->
        { clickedDisplay -> callback(LibStringAction.MetadataResourceClicked(item, clickedDisplay)) }
      }
    )
  }

  private fun bindHighlightBackground(holder: BaseViewHolder) {
    if (
      renderState.highlightPosition == LibStringRenderState.NO_HIGHLIGHT_POSITION ||
      holder.absoluteAdapterPosition != renderState.highlightPosition
    ) {
      if (holder.itemView.background is TransitionDrawable) {
        (holder.itemView.background as TransitionDrawable).reverseTransition(
          HIGHLIGHT_TRANSITION_DURATION
        )
        holder.itemView.setBackgroundResource(
          context.getResourceIdByAttr(android.R.attr.selectableItemBackground)
        )
      }
      return
    }

    val drawable = TransitionDrawable(
      listOf(
        Color.TRANSPARENT.toDrawable(),
        context.getColorByAttr(com.google.android.material.R.attr.colorSecondaryContainer)
          .toDrawable()
      ).toTypedArray()
    )
    holder.itemView.background = drawable
    drawable.startTransition(HIGHLIGHT_TRANSITION_DURATION)
  }

  private fun isItemOptionEnabled(option: Int): Boolean {
    return (renderState.itemDisplayOptions and option) > 0
  }

  private fun LibStringItemChip.metadataPreviewKey(): MetadataPreviewKey {
    return MetadataPreviewKey(
      name = item.name,
      resourceId = item.size,
      resourceType = labels.firstOrNull(),
      originalValue = item.source
    )
  }

  private data class MetadataPreviewKey(
    val name: String,
    val resourceId: Long,
    val resourceType: String?,
    val originalValue: String?
  )
}
