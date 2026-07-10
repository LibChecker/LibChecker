package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.action.AppInfoActionItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInfoItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoAdapter(
  private val onActionClick: (AppInfoActionItem) -> Unit
) : BaseQuickAdapter<AppInfoActionItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppInfoItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppInfoActionItem) {
    (holder.itemView as AppInfoItemView).bind(item, onActionClick)
  }
}
