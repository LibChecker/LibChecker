package com.absinthe.libchecker.recyclerview.adapter.detail

import android.view.ViewGroup
import com.absinthe.libchecker.bean.AlternativeLaunchItem
import com.absinthe.libchecker.view.detail.AlternativeLaunchItemView
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
