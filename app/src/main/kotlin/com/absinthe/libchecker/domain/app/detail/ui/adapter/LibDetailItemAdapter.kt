package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.ui.view.DetailInfoItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibDetailItemAdapter : BaseQuickAdapter<DetailInfoItemDisplay, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(DetailInfoItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: DetailInfoItemDisplay) {
    (holder.itemView as DetailInfoItemView).bind(item)
  }
}
