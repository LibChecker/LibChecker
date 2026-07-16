package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AlternativeLaunchItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AlternativeLaunchAdapter : BaseQuickAdapter<AlternativeLaunchItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AlternativeLaunchItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AlternativeLaunchItem) {
    (holder.itemView as AlternativeLaunchItemView).bind(item)
  }
}
