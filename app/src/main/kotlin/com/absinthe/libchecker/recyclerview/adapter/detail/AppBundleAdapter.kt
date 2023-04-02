package com.absinthe.libchecker.recyclerview.adapter.detail

import android.text.format.Formatter
import android.view.ViewGroup
import com.absinthe.libchecker.model.AppBundleItem
import com.absinthe.libchecker.view.detail.AppBundleItemView
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
      setSizeText(Formatter.formatFileSize(context, item.size))
    }
  }
}
