package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AppPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppPropsAdapter(
  private val onResourceClick: (AppPropItem) -> Unit
) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppPropItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppPropItem) {
    (holder.itemView as AppPropItemView).bind(item, onResourceClick)
  }

  fun replace(item: AppPropItem) {
    data.indexOfFirst { it.key == item.key }
      .takeIf { it >= 0 }
      ?.let { position ->
        data[position] = item
        notifyItemChanged(position)
      }
  }
}
