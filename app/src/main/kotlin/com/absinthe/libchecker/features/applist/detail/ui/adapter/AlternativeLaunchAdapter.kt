package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.features.applist.detail.bean.AlternativeLaunchItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AlternativeLaunchItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AlternativeLaunchAdapter : BaseQuickAdapter<AlternativeLaunchItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AlternativeLaunchItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AlternativeLaunchItem) {
    (holder.itemView as AlternativeLaunchItemView).apply {
      label.text = item.label
      className.text = item.className
    }
  }
}
