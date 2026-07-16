package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AppBundleItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppBundleAdapter : BaseQuickAdapter<AppBundleItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppBundleItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppBundleItem) {
    (holder.itemView as AppBundleItemView).bind(item)
  }
}
