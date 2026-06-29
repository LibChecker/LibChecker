package com.absinthe.libchecker.domain.snapshot.track.ui.adapter

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.track.model.TrackedAppListItem
import com.absinthe.libchecker.domain.snapshot.track.ui.view.TrackItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class TrackAdapter : BaseQuickAdapter<TrackedAppListItem, BaseViewHolder>(0) {

  init {
    addChildClickViewIds(android.R.id.toggle)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      TrackItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(0, margin, 0, margin)
        }
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: TrackedAppListItem) {
    (holder.itemView as TrackItemView).container.apply {
      icon.load(item.packageInfo)
      appName.text = item.label
      packageName.text = item.packageName
      switch.isChecked = item.switchState
      switch.contentDescription = buildItemDescription(item.label, item.packageName)
    }
  }
}

private fun buildItemDescription(vararg parts: CharSequence?): String {
  return parts
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}
