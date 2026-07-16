package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarAction
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AppDetailToolbarView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppDetailToolbarAdapter(
  private val onActionClick: (AppDetailToolbarAction) -> Unit
) : BaseQuickAdapter<AppDetailToolbarItem, BaseViewHolder>(0) {

  init {
    setHasStableIds(true)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppDetailToolbarView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppDetailToolbarItem) {
    (holder.itemView as AppDetailToolbarView).bind(item, onActionClick)
  }

  override fun getItemId(position: Int): Long {
    return data[position].stableId
  }
}
