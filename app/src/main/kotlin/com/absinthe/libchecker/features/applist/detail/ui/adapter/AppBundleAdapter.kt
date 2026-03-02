package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.features.applist.detail.bean.AppBundleItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AppBundleItemView
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppBundleAdapter : BaseQuickAdapter<AppBundleItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppBundleItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppBundleItem) {
    (holder.itemView as AppBundleItemView).apply {
      setIcon(item.type)
      setNameText(item.name)
      setSizeText(item.size.sizeToString(context, showBytes = false))
    }
  }
}
